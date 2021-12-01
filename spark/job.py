import time
import os
from pyspark.sql import SparkSession

MONGO_CONNECTION_STR = os.environ.get('MONGO_CONNECTION_STRING')
MONGO_DB = os.environ.get('MONGO_DB')

if MONGO_CONNECTION_STR == None or MONGO_DB == None:
    raise Exception('Please define the MONGO_CONNECTION_STRING and MONGO_DB environment variables')

def getSparkSession(session_name: str, input_col: str) -> SparkSession:
    input_uri = f'{MONGO_CONNECTION_STR}/{MONGO_DB}.{input_col}'
    output_uri = f'{MONGO_CONNECTION_STR}/{MONGO_DB}.{session_name}'

    return SparkSession \
        .builder \
        .appName(session_name) \
        .config('spark.mongodb.input.uri', input_uri) \
        .config('spark.mongodb.output.uri', output_uri) \
        .getOrCreate()

# Compute trending channels:
# 1. Channels with most messages in the last day

tc_session = getSparkSession('trendingChannel', 'channelMessage')
tc_df = tc_session.read \
    .format('mongo') \
    .load()

cur_time = time.time() * 1000
last_day = cur_time - 86400000 # A day is 86400000 milliseconds

tc_df.createOrReplaceTempView('tc_view')
tc = tc_session.sql(f'''
    select channelId, count(*) as msgCount 
    from tc_view 
    where createdAt >= {last_day}
    group by channelId
    order by count(*)
''')

print('\n------ Trending Channels -------')
tc.show()

tc.write \
    .format('mongo') \
    .mode('append') \
    .save()