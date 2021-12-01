az &> /dev/null

# Check whether the azure-cli is installed in the system
if [[ $? -ne 0 ]]; then
    echo "Please install the azure-cli on your system"
    exit 1
fi

az login --use-device-code

# Check for login error
if [[ $? -ne 0 ]]; then
    echo "Error while logging in to azure"
    exit 1
fi

# The user needs to provide at least one region for deployment
if [[ $# -eq 0 ]]; then
    echo "Please specifiy at least one deployment region: ${0} {regions...}"
    exit 1
fi

RESOURCE_GROUP_PREFIX="scc62241"
DOCKER_IMAGE_NAME="xploitedd/scc-backend"

# ------------------------------------------------------------------------
#       MONGODB CREATION
# ------------------------------------------------------------------------

create_cosmosdb() {
    echo -e "Creating CosmosDB database with locations:\n${2}"

    resource_group="${RESOURCE_GROUP_PREFIX}${1}"

    az group create --name ${resource_group} --location "${1}" &> /dev/null
    az cosmosdb create \
        --name "db${resource_group}" \
        --resource-group "${resource_group}" \
        --enable-free-tier true \
        --enable-multiple-write-locations true \
        --enable-public-network true \
        --backup-redundancy Local \
        --default-consistency-level BoundedStaleness \
        --max-interval 300 \
        --max-staleness-prefix 100000 \
        --kind MongoDB ${2} &> /dev/null
}

fp=0
mongodb_locations=""
for region in $@;
do
    mongodb_locations+="--locations regionName=${region} failoverPriority=${fp} "
    ((fp++))
done

create_cosmosdb "${1}" "${mongodb_locations}"

# ------------------------------------------------------------------------
#       MONGO DB PROPERTIES
# ------------------------------------------------------------------------

MASTER_RESOURCE_GROUP="${RESOURCE_GROUP_PREFIX}${1}"
COSMOS_INSTANCE_NAME="db${MASTER_RESOURCE_GROUP}"
COSMOS_PRIMARY_KEY=$(az cosmosdb keys list --name ${COSMOS_INSTANCE_NAME} --resource-group ${MASTER_RESOURCE_GROUP} --query primaryMasterKey | tr -d '"' | tr -d '\r')
MONGO_CONNECTION_STR="mongodb://${COSMOS_INSTANCE_NAME}:${COSMOS_PRIMARY_KEY}@${COSMOS_INSTANCE_NAME}.mongo.cosmos.azure.com:10255/?ssl=true&retrywrites=false&replicaSet=globaldb&maxIdleTimeMS=120000&appName=@${COSMOS_INSTANCE_NAME}@"
MONGO_DB="scc"

# ------------------------------------------------------------------------
#       STORAGE PROPERTIES
# ------------------------------------------------------------------------

STORAGE_CONTAINER_NAME="media"

# ------------------------------------------------------------------------
#       CREATION METHODS
# ------------------------------------------------------------------------

create_app_service_plan() {
    echo "Creating app service plan ${1} in resource group ${2}..."

    az appservice plan create \
        --name "${1}" \
        --resource-group "${2}" \
        --is-linux \
        --sku B1 &> /dev/null

    if [[ $? -ne 0 ]]; then
        echo "Error creating app service plan ${1}"
        exit 1
    fi
}

create_storage_container() {
    echo "Creating ${STORAGE_CONTAINER_NAME} container in storage account ${1}"

    az storage container create \
        --name ${STORAGE_CONTAINER_NAME} \
        --account-name "${1}" \
        --public-access blob &> /dev/null

    if [[ $? -ne 0 ]]; then
        echo "Error creating ${STORAGE_CONTAINER_NAME} container for storage account ${1}"
        return
    fi
}

deploy_storage_account() {
    echo "Creating storage account ${1}"

    az storage account check-name --name "${1}" | grep -o false &> /dev/null
    if [[ $? -eq 0 ]]; then
        echo "A storage account named ${1} already exists!"
        create_storage_container "${1}"
        return
    fi

    az storage account create \
        --name "${1}" \
        --resource-group "${1}" \
        --kind StorageV2 \
        --sku Standard_LRS \
        --access-tier Hot &> /dev/null

    if [[ $? -ne 0 ]]; then
        echo "An error occurred while creating a storage account: ${1}"
        return
    fi

    connection_str=$(az storage account show-connection-string --name ${1} --query connectionString)

    queue_name="q${1}"
    az storage queue create \
        --name "${queue_name}" \
        --connection-string ${connection_str} &> /dev/null

    if [[ $? -ne 0 ]]; then
        echo "Error occurred while creating storage queue for account ${1}"
    fi

    create_storage_container "${1}"
}

deploy_redis_instance() {
    echo "Deploying redis instance to region ${2}"

    redis_name="r${1}"
    az redis create \
        --location "${2}" \
        --name "${redis_name}" \
        --resource-group "${1}" \
        --sku Basic \
        --vm-size c0 &> /dev/null

    if [[ $? -ne 0 ]]; then
        echo "Error creating redis instance ${redis_name}"
        return
    fi
}

deploy_functions() {
    func_name="f${1}"
    plan="p${func_name}"

    create_app_service_plan "${plan}" "${1}"

    echo "Creating function app ${func_name}..."

    az functionapp create \
        --name "${func_name}" \
        --resource-group "${1}" \
        --storage-account "${1}" \
        --functions-version 4 \
        --os-type Linux \
        --runtime Node \
        --runtime-version 14 \
        --plan ${plan} &> /dev/null

    if [[ $? -ne 0 ]]; then
        echo "Error creating function app ${func_name}"
        return
    fi

    return
}

deploy_backend_app() {
    webapp_name="app${1}"
    plan="p${webapp_name}"

    create_app_service_plan "${plan}" "${1}"

    az webapp create \
        --name "${webapp_name}" \
        --plan "${plan}" \
        --resource-group "${1}" \
        --deployment-container-image-name "${DOCKER_IMAGE_NAME}" &> /dev/null

    if [[ $? -ne 0 ]]; then
        echo "Error creating web app ${webapp_name}"
        return
    fi

    return
}

configure_env_variables() {
    # We need to configure the environment variables for a app service and the functions
    # of a specific region

    redis_instance_name="r${1}"
    redis_host=$(az redis show --name ${redis_instance_name} --resource-group ${1} --query "join(':', [hostName, to_string(sslPort)])" | tr -d '"')
    redis_primary_key=$(az redis list-keys --name ${redis_instance_name} --resource-group ${1} --query primaryKey | tr -d '"' | tr -d '\r')
    redis_connection_str="rediss://${redis_primary_key}@${redis_host}"
    storage_connection_str=$(az storage account show-connection-string --name ${1} --query connectionString | tr -d '"')

    # App Service variables:
    # - MongoDB Connection String
    # - Database Name
    # - Redis Connection String
    # - Storage Connection String
    # - Storage Container Name

    webapp_name="app${1}"
    
    az webapp config appsettings set \
        --name "${webapp_name}" \
        --resource-group "${1}" \
        --settings DB_CONNECTION_STRING="${MONGO_CONNECTION_STR}" \
                   DB_NAME="${MONGO_DB}" \
                   STORAGE_CONNECTION_STRING=${storage_connection_str} \
                   STORAGE_CONTAINER=${STORAGE_CONTAINER_NAME} \
                   REDIS_CONNECTION_STRING=${redis_connection_str} \
                   RESOURCE_PREFIX=${RESOURCE_GROUP_PREFIX} \
                   WEBSITES_PORT=8080 &> /dev/null

    if [[ $? -ne 0 ]]; then
        echo 'Error creating environment variables for appservice'
    fi

    # Function Variables:
    # - MongoDB Connection String

    func_name="f${1}"

    az functionapp config appsettings set \
        --name "${func_name}" \
        --resource-group "${1}" \
        --settings MONGO_CONNECTION_STR=${MONGO_CONNECTION_STR} &> /dev/null

    if [[ $? -ne 0 ]]; then
        echo 'Error creating environment variables for functions'
    fi
}

deploy_to_region() {
    echo -e "\n---- Deploying to region ${1} ----"

    resource_group="${RESOURCE_GROUP_PREFIX}${1}"

    # Create the resource group for the location
    echo "Creating the resource group ${resource_group}"
    az group create --name ${resource_group} --location "${1}" &> /dev/null

    # We need to deploy the storage account, the functions and the backend app
    deploy_storage_account "${resource_group}" "${1}"
    deploy_redis_instance "${resource_group}" "${1}"
    deploy_functions "${resource_group}" "${1}"
    deploy_backend_app "${resource_group}" "${1}" "${2}"
    configure_env_variables "${resource_group}" "${1}"
}

for region in $@;
do
    deploy_to_region "$region" "${1}"
done
