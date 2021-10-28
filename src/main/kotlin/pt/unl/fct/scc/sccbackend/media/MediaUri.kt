package pt.unl.fct.scc.sccbackend.media

import org.springframework.web.util.UriTemplate


object MediaUri {

    const val MEDIAS = "/media"
    const val MEDIA  = "$MEDIAS/{blobName}"


    fun forMedia(blobName: String) = UriTemplate(MEDIA).expand(blobName)
}