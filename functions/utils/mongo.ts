import { Db, MongoClient } from "mongodb"

const DEFAULT_DB_NAME = 'scc'

export type MongoScope = (db: Db) => Promise<void>

export const createMongoClient = async (scope: MongoScope): Promise<void> => {
    const connectionString = process.env.MONGO_CONNECTION_STR
    if (!connectionString)
        throw new Error('Please define the MONGO_CONNECTION_STR environment variable!')

    const client = new MongoClient(connectionString)
    await client.connect()

    const session = client.startSession()
    try {
        await session.withTransaction(async () => {
            const dbName = process.env.MONGO_DB_NAME || DEFAULT_DB_NAME
            await scope(client.db(dbName))
        })
    } finally {
        session.endSession()
        client.close()
    }
}