export interface User {
    _id: string
    nickname: string
    name: string
    password: string
    photo?: string
    deleted: boolean
}

export interface Channel {
    _id: string
    name: string
    owner: string
    private: boolean
    deleted: boolean
}

export interface Message {
    _id: string
    channelId: string
    user: string
    text: string
    media?: string
    replyTo?: string
    createdAt: number
    deleted: boolean
}

export interface UserChannel {
    _id: string
    channel: string
    user: string
}

export interface Media {
    _id: string
    contentType: string
    hash: string
}