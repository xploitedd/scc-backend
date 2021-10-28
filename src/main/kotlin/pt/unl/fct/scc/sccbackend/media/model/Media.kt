package pt.unl.fct.scc.sccbackend.media.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



@Serializable
class Media (
    @SerialName("_id") val blobName: String,
    val content:    String
)