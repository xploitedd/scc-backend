import { AzureFunction, Context } from "@azure/functions"
import { Db } from "mongodb"
import { Channel, Media, Message, User, UserChannel } from "../utils/model"
import { createMongoClient } from "../utils/mongo"

type CleanupFunction = (db: Db) => Promise<void>

const timerTrigger: AzureFunction = async function (context: Context, myTimer: any): Promise<void> { 
    const time = Date.now()
    console.log(`Running function at ${time}`)

    await createMongoClient(async db => {
        await runCleanup('Users', db, cleanupUsers)
        await runCleanup('Messages', db, cleanupMessages)
        await runCleanup('Channels', db, cleanupChannels)
    })
}

const runCleanup = async (name: string, db: Db, func: CleanupFunction): Promise<void> => {
    const startTime = Date.now()
    console.log(`Running ${name} cleanup...`)

    await func(db)

    const took = Date.now() - startTime
    console.log(`Finished ${name} cleanup in ${took}ms`)
}

const cleanupUsers: CleanupFunction = async (db: Db): Promise<void> => {
    // When a user is deleted:
    // 1. The messages the user sent should specify the Deleted user
    // 2. Remove the user from the channels
    // 3. Delete the user permanently

    const deletedUsers = await db.collection<User>('user')
        .find({ deleted: true })
        .toArray()

    deletedUsers.forEach(async user => {
        console.log(`User ${user.nickname} (${user._id}) is being deleted`)

        if (user.photo)
            await db.collection<Media>('media').deleteOne({ _id: user.photo })

        await db.collection<Message>('channelMessage').updateMany({ user: user.nickname, deleted: false }, { $set: { user: 'Deleted' } })
        await db.collection<UserChannel>('userChannel').deleteMany({ user: user._id })
        await db.collection<User>('user').deleteOne({ _id: user._id })
    })
}

const cleanupMessages: CleanupFunction = async (db: Db): Promise<void> => {
    // When a message is deleted:
    // 1. If the message is part of a reply context, then update the reply context to remove the link
    // 2. If the message has a media object, then delete it
    // 3. Delete the message

    const deletedMessages = await db.collection<Message>('channelMessage')
        .find({ deleted: true })
        .toArray()

    deletedMessages.forEach(async msg => {
        console.log(`Message ${msg._id} is being deleted`)

        if (msg.media)
            await db.collection<Media>('media').deleteOne({ _id: msg.media })

        await db.collection<Message>('channelMessage').updateMany({ replyTo: msg._id }, { $set: { replyTo: 'Deleted' } })
        await db.collection<Message>('channelMessage').deleteOne({ _id: msg._id })
    })
}

const cleanupChannels: CleanupFunction = async (db: Db): Promise<void> => {
    // When a channel is deleted:
    // 1. The user-channel relation has already been deleted, to avoid ghost channels
    // 2. We need to delete the channel messages
    // 3. Delete the channel

    const deletedChannels = await db.collection<Channel>('channel')
        .find({ deleted: true })
        .toArray()

    deletedChannels.forEach(async channel => {
        console.log(`Channel ${channel._id} is being deleted`)

        await db.collection<Message>('channelMessage').deleteMany({ channelId: channel._id })
        await db.collection<Channel>('channel').deleteOne({ _id: channel._id })
    })
}

export default timerTrigger
