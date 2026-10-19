package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreMailConfig(
    @DocumentId val id: String = "settings",
    val senderEmail: String = "",
    val senderName: String = "",
    val toGroup: String = "",
    val ccGroup: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
