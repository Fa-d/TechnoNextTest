package dev.sadakat.technonexttest.domain.usecase.auth

import dev.sadakat.technonexttest.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.logout()
    }
}