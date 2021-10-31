package pt.unl.fct.scc.sccbackend.media.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.newId

@Serializable
class Media (
    val mediaType: String,
    @SerialName("_id")
    val blobName: String = newId<Media>().toString()
)