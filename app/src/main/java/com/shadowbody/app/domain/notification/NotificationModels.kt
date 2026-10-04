package com.shadowbody.app.domain.notification

data class NotificationContent(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val priority: Int,
)

data class NotificationState(
    val notifications: List<NotificationContent>,
    val topNotification: NotificationContent?,
)
