package pt.unl.fct.scc.sccbackend.media.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.newId

@Serializable
class Media(
    val contentType: String,
    val hash: String,
    @SerialName("_id")
    val mediaId: String = newId<Media>().toString()
)

class MediaDto(
    val mediaId: String,
    val contentType: String,
    val hash: String
)

fun Media.toMediaDto() = MediaDto(
    mediaId,
    contentType,
    hash
)