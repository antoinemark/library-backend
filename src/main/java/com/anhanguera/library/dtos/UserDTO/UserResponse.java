package com.anhanguera.library.dtos.UserDTO;

public record UserResponse(
        Long id,
        String name,
        // Somente dígitos
        String cpf,
        // false quando o usuário possui empréstimo em aberto com a devolução atrasada
        boolean debtFree
) {}
