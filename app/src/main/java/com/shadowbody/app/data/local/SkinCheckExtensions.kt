package com.shadowbody.app.data.local

/**
 * Feature B helpers kept outside the entity so the Room schema stays 8
 * columns exactly as the v12 migration creates them.
 */
object SkinCheckMethods {
    const val CAMERA = "CAMERA"
    const val GALLERY = "GALLERY"
}

/** The daily check is complete only when BOTH cheek photos exist. */
val SkinCheckRecord.isComplete: Boolean
    get() = leftCheekPhotoPath != null && rightCheekPhotoPath != null
