package com.anhanguera.library.service;

import com.anhanguera.library.dtos.UserDTO.UserRequest;
import com.anhanguera.library.dtos.UserDTO.UserResponse;
import com.anhanguera.library.exception.BusinessRuleException;
import com.anhanguera.library.exception.ResourceNotFoundException;
import com.anhanguera.library.model.User;
import com.anhanguera.library.repository.LendRepository;
import com.anhanguera.library.repository.UserRepository;
import com.anhanguera.library.util.AlphabeticalOrder;
import com.anhanguera.library.validation.CpfValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DUPLICATED_CPF_MESSAGE = "Já existe um usuário cadastrado com este CPF.";

    private final UserRepository userRepository;
    private final LendRepository lendRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .sorted(AlphabeticalOrder.by(User::getName).thenComparing(User::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findUser(id));
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        String cpf = CpfValidator.normalize(request.cpf());
        if (userRepository.existsByCpf(cpf)) {
            throw new BusinessRuleException(DUPLICATED_CPF_MESSAGE);
        }
        User user = new User();
        user.setName(request.name());
        user.setCpf(cpf);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = findUser(id);
        String cpf = CpfValidator.normalize(request.cpf());
        if (userRepository.existsByCpfAndIdNot(cpf, id)) {
            throw new BusinessRuleException(DUPLICATED_CPF_MESSAGE);
        }
        user.setName(request.name());
        user.setCpf(cpf);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUser(id);
        // O histórico de empréstimos é preservado, então usuários com empréstimos não podem ser excluídos
        if (lendRepository.existsByUserId(id)) {
            throw new BusinessRuleException("Não é possível excluir o usuário, pois ele possui empréstimos registrados.");
        }
        userRepository.delete(user);
    }

    /**
     * Busca a entidade ou lança 404. Também usada pelo LendingService.
     */
    @Transactional(readOnly = true)
    public User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    /**
     * Indica se o usuário possui empréstimo em aberto com a data prevista de devolução já vencida.
     */
    @Transactional(readOnly = true)
    public boolean hasOverdueLendings(Long userId) {
        return lendRepository.existsByUserIdAndReturnDateIsNullAndEndDateBefore(userId, LocalDate.now(clock));
    }

    /**
     * Converte a entidade para o DTO de resposta. Também usada pelo LendingService.
     */
    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getCpf(),
                !hasOverdueLendings(user.getId())
        );
    }
}
