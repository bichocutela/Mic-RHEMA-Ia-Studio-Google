package com.aistudio.micrhema

/**
 * Modelo dedicado aos eventos temporários da igreja.
 *
 * Ele é propositalmente separado de [ChurchService]: cultos fixos continuam usando
 * a agenda recorrente atual, enquanto eventos podem possuir período, banner e
 * informações específicas sem interferir na lógica de próximo culto.
 *
 * Todos os campos possuem valores padrão para manter a leitura do Firestore
 * tolerante a documentos antigos ou parcialmente preenchidos.
 */
data class ChurchEventModel(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var preacher: String = "",
    var startDate: String = "",
    var endDate: String = "",
    var time: String = "",
    var location: String = "",
    var bannerUrl: String = "",
    var isPublished: Boolean = true,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L
) {
    /** Evento de um único dia quando não há término ou o término é igual ao início. */
    fun isSingleDay(): Boolean = endDate.isBlank() || endDate == startDate
}

/**
 * Estado novo usado pelas próximas etapas da aba Cultos.
 * O estado legado [eventsState] permanece intacto para não quebrar telas atuais.
 */
val churchEventsState = androidx.compose.runtime.mutableStateListOf<ChurchEventModel>()
