package fr.univ.nantes.feature.profil


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ProfilScreen(viewModel: ProfilViewModel) {
    // On observe l'état du profil
    val profilSaved by viewModel.state.collectAsState()

    // States locaux pour la saisie (se synchronisent si profilSaved change)
    var name by remember(profilSaved.name) { mutableStateOf(profilSaved.name) }
    var firstname by remember(profilSaved.firstname) { mutableStateOf(profilSaved.firstname) }
    var email by remember(profilSaved.email) { mutableStateOf(profilSaved.email) }

    var errors by remember { mutableStateOf(mapOf<String, String>()) }

    Column(modifier = Modifier.padding(16.dp)) {
        // Champ Prénom
        OutlinedTextField(
            value = firstname,
            onValueChange = { firstname = it },
            label = { Text("Prénom") },
            isError = errors.containsKey("firstname"),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Champ Nom
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nom") },
            isError = errors.containsKey("name"),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Champ Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            isError = errors.containsKey("email"),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val validationErrors = viewModel.validateProfil(name, firstname, email)
                if (validationErrors.isEmpty()) {
                    viewModel.saveProfil(name, firstname, email)
                    errors = emptyMap()
                } else {
                    errors = validationErrors
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enregistrer")
        }

    }
}

@Preview(showBackground = true, name = "Écran Profil Vide")
@Composable
fun ProfilPreviewEmpty() {
    MaterialTheme {
        ProfilScreen(
            viewModel = ProfilViewModel().apply {
                // On s'assure que le profil est vide
                saveProfil("", "", "")
            }
        )
    }
}
