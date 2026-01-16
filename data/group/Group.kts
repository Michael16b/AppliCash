package fr.univ.nantes.data.group

data class Group(
    val id: String,
    val title: String,
    val members: List<String>
)