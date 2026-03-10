package fr.univ.nantes.data.expense.repository

import fr.univ.nantes.data.expense.dao.ExpenseDao
import fr.univ.nantes.data.expense.dao.ExpenseGroupDao
import fr.univ.nantes.data.expense.dao.ParticipantDao
import fr.univ.nantes.data.expense.entity.ExpenseEntity
import fr.univ.nantes.data.expense.entity.ExpenseGroupEntity
import fr.univ.nantes.data.expense.entity.ParticipantEntity
import fr.univ.nantes.data.expense.model.GroupWithDetails
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ExpenseRepositoryImplTest {

    // ── Mocks ──────────────────────────────────────────────────────────────────
    private lateinit var groupDao: ExpenseGroupDao
    private lateinit var participantDao: ParticipantDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        groupDao = mock()
        participantDao = mock()
        expenseDao = mock()
        repository = ExpenseRepositoryImpl(groupDao, participantDao, expenseDao)
    }

    // ── createGroup ─────────────────────────────────────────────────────────────

    @Test
    fun `createGroup avec 2 membres valides retourne l id du groupe`() = runTest {
        whenever(groupDao.insertGroup(any())).thenReturn(1L)

        val id = repository.createGroup("Vacances", listOf("Alice", "Bob"))

        assertEquals(1L, id)
        verify(groupDao).insertGroup(any())
        verify(participantDao).insertParticipants(any())
    }

    @Test
    fun `createGroup insere exactement les participants fournis`() = runTest {
        whenever(groupDao.insertGroup(any())).thenReturn(42L)
        val captor = argumentCaptor<List<ParticipantEntity>>()

        repository.createGroup("Trip", listOf("Alice", "Bob", "Charlie"))

        verify(participantDao).insertParticipants(captor.capture())
        val inserted = captor.firstValue
        assertEquals(3, inserted.size)
        assertTrue(inserted.all { it.groupId == 42L })
        assertEquals(listOf("Alice", "Bob", "Charlie"), inserted.map { it.name })
    }

    // ── RG1 : au moins 2 membres ──────────────────────────────────────────────

    @Test
    fun `RG1 - createGroup avec 0 membre leve NotEnoughMembersException`() = runTest {
        try {
            repository.createGroup("Seul", emptyList())
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.NotEnoughMembersException) {
            assertNotNull(e)
        }
        verify(groupDao, never()).insertGroup(any())
    }

    @Test
    fun `RG1 - createGroup avec 1 membre leve NotEnoughMembersException`() = runTest {
        try {
            repository.createGroup("Solo", listOf("Alice"))
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.NotEnoughMembersException) {
            assertNotNull(e)
        }
    }

    // ── RG2 : nom de membre non vide ──────────────────────────────────────────

    @Test
    fun `RG2 - createGroup avec un nom vide leve EmptyMemberNameException`() = runTest {
        try {
            repository.createGroup("Groupe", listOf("Alice", ""))
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.EmptyMemberNameException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `RG2 - createGroup avec un nom blank leve EmptyMemberNameException`() = runTest {
        try {
            repository.createGroup("Groupe", listOf("Alice", "   "))
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.EmptyMemberNameException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `RG2 - addParticipantToGroup avec nom vide leve EmptyMemberNameException`() = runTest {
        try {
            repository.addParticipantToGroup(1L, "")
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.EmptyMemberNameException) {
            assertNotNull(e)
        }
        verify(participantDao, never()).insertParticipant(any())
    }

    // ── RG3 : noms uniques dans un groupe ─────────────────────────────────────

    @Test
    fun `RG3 - createGroup avec doublons leve DuplicateMemberNameException`() = runTest {
        try {
            repository.createGroup("Groupe", listOf("Alice", "Bob", "Alice"))
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.DuplicateMemberNameException) {
            assertTrue(e.message?.contains("Alice") == true)
        }
    }

    @Test
    fun `RG3 - addParticipantToGroup avec nom existant leve DuplicateMemberNameException`() = runTest {
        whenever(participantDao.getParticipantsByGroupId(1L)).thenReturn(
            listOf(ParticipantEntity(id = 1, groupId = 1L, name = "Alice"))
        )

        try {
            repository.addParticipantToGroup(1L, "Alice")
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.DuplicateMemberNameException) {
            assertTrue(e.message?.contains("Alice") == true)
        }
        verify(participantDao, never()).insertParticipant(any())
    }

    @Test
    fun `RG3 - addParticipantToGroup avec nom unique insere le participant`() = runTest {
        whenever(participantDao.getParticipantsByGroupId(1L)).thenReturn(
            listOf(ParticipantEntity(id = 1, groupId = 1L, name = "Alice"))
        )

        repository.addParticipantToGroup(1L, "Bob")

        verify(participantDao).insertParticipant(ParticipantEntity(groupId = 1L, name = "Bob"))
    }

    // ── RG4 : montant > 0 ────────────────────────────────────────────────────

    @Test
    fun `RG4 - addExpenseToGroup avec montant zero leve InvalidAmountException`() = runTest {
        try {
            repository.addExpenseToGroup(1L, "Diner", 0.0, "Alice")
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.InvalidAmountException) {
            assertNotNull(e)
        }
        verify(expenseDao, never()).insertExpense(any())
    }

    @Test
    fun `RG4 - addExpenseToGroup avec montant negatif leve InvalidAmountException`() = runTest {
        try {
            repository.addExpenseToGroup(1L, "Diner", -5.0, "Alice")
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.InvalidAmountException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `RG4 - addExpenseToGroup avec montant positif insere la depense`() = runTest {
        val captor = argumentCaptor<ExpenseEntity>()

        repository.addExpenseToGroup(1L, "Diner", 42.50, "Alice")

        verify(expenseDao).insertExpense(captor.capture())
        assertEquals(42.50, captor.firstValue.amount, 0.001)
        assertEquals("Diner", captor.firstValue.description)
        assertEquals("Alice", captor.firstValue.paidBy)
    }

    // ── RG5 : suppression d'un membre avec dépenses ───────────────────────────

    @Test
    fun `RG5 - removeParticipantFromGroup avec depenses leve MemberHasExpensesException`() = runTest {
        whenever(expenseDao.getExpensesByGroupId(1L)).thenReturn(
            listOf(
                ExpenseEntity(id = 1, groupId = 1L, description = "Taxi",
                    amount = 20.0, paidBy = "Alice")
            )
        )

        try {
            repository.removeParticipantFromGroup(1L, "Alice")
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.MemberHasExpensesException) {
            assertTrue(e.message?.contains("Alice") == true)
        }
        verify(participantDao, never()).deleteParticipantByName(any(), any())
    }

    @Test
    fun `RG5 - removeParticipantFromGroup sans depenses supprime le membre`() = runTest {
        whenever(expenseDao.getExpensesByGroupId(1L)).thenReturn(
            listOf(
                ExpenseEntity(id = 1, groupId = 1L, description = "Taxi",
                    amount = 20.0, paidBy = "Bob")
            )
        )

        repository.removeParticipantFromGroup(1L, "Alice")

        verify(participantDao).deleteParticipantByName(1L, "Alice")
    }

    @Test
    fun `RG5 - removeParticipantFromGroup sans aucune depense supprime le membre`() = runTest {
        whenever(expenseDao.getExpensesByGroupId(1L)).thenReturn(emptyList())

        repository.removeParticipantFromGroup(1L, "Alice")

        verify(participantDao).deleteParticipantByName(1L, "Alice")
    }

    // ── updateGroup ────────────────────────────────────────────────────────────

    @Test
    fun `updateGroup avec nouveau nom met a jour le nom`() = runTest {
        whenever(expenseDao.getExpensesByGroupId(1L)).thenReturn(emptyList())

        repository.updateGroup(1L, "Nouveau Nom", emptyList(), emptyList())

        verify(groupDao).updateGroupName(1L, "Nouveau Nom")
    }

    @Test
    fun `updateGroup sans nouveau nom ne met pas a jour le nom`() = runTest {
        whenever(expenseDao.getExpensesByGroupId(1L)).thenReturn(emptyList())

        repository.updateGroup(1L, null, emptyList(), emptyList())

        verify(groupDao, never()).updateGroupName(any(), any())
    }

    @Test
    fun `RG5 - updateGroup avec suppression membre ayant depenses leve MemberHasExpensesException`() = runTest {
        whenever(expenseDao.getExpensesByGroupId(1L)).thenReturn(
            listOf(
                ExpenseEntity(id = 1, groupId = 1L, description = "Hotel",
                    amount = 100.0, paidBy = "Alice")
            )
        )

        try {
            repository.updateGroup(1L, null, emptyList(), listOf("Alice"))
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.MemberHasExpensesException) {
            assertTrue(e.message?.contains("Alice") == true)
        }
        verify(participantDao, never()).updateParticipants(any(), any(), any())
    }

    @Test
    fun `RG2 - updateGroup avec nouveau membre vide leve EmptyMemberNameException`() = runTest {
        try {
            repository.updateGroup(1L, null, listOf(""), emptyList())
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.EmptyMemberNameException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `RG3 - updateGroup avec doublons dans les ajouts leve DuplicateMemberNameException`() = runTest {
        try {
            repository.updateGroup(1L, null, listOf("Charlie", "Charlie"), emptyList())
            fail("Exception attendue")
        } catch (e: ExpenseBusinessException.DuplicateMemberNameException) {
            assertTrue(e.message?.contains("Charlie") == true)
        }
    }

    // ── deleteGroup / deleteExpense ────────────────────────────────────────────

    @Test
    fun `deleteGroup appelle le dao avec le bon id`() = runTest {
        repository.deleteGroup(5L)
        verify(groupDao).deleteGroup(5L)
    }

    @Test
    fun `deleteExpense appelle le dao avec le bon id`() = runTest {
        repository.deleteExpense(7L)
        verify(expenseDao).deleteExpense(7L)
    }

    @Test
    fun `updateGroupName appelle le dao avec les bons parametres`() = runTest {
        repository.updateGroupName(3L, "Nouveau")
        verify(groupDao).updateGroupName(3L, "Nouveau")
    }

    // ── getGroupWithDetails ───────────────────────────────────────────────────

    @Test
    fun `getGroupWithDetails retourne null quand le groupe n existe pas`() = runTest {
        whenever(groupDao.getGroupWithDetails(99L)).thenReturn(null)

        val result = repository.getGroupWithDetails(99L)

        assertEquals(null, result)
    }

    @Test
    fun `getGroupWithDetails retourne le groupe avec ses details`() = runTest {
        val group = ExpenseGroupEntity(id = 1L, groupName = "Vacances")
        val expected = GroupWithDetails(group = group, participants = emptyList(), expenses = emptyList())
        whenever(groupDao.getGroupWithDetails(1L)).thenReturn(expected)

        val result = repository.getGroupWithDetails(1L)

        assertEquals(expected, result)
    }
}

