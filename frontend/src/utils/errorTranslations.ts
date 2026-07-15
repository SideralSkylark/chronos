const errorTranslations: Record<string, string> = {
  'Invalid credentials': 'Credenciais inválidas.',
  'User not found': 'Utilizador não encontrado.',
  'Account not verified. Please verify your email first.':
    'Conta não verificada. Por favor, verifique o seu email primeiro.',
  'Invalid or expired refresh token': 'Sessão expirada. Por favor, faça login novamente.',
  'Access denied': 'Acesso negado.',
  'Resource not found': 'Recurso não encontrado.',
  'The requested resource was not found': 'O recurso solicitado não foi encontrado.',
  'A resource with conflicting data already exists': 'Já existe um recurso com dados conflituosos.',
  'Request violates a data constraint': 'O pedido viola uma restrição de dados.',
  'Malformed JSON or invalid request body': 'Corpo do pedido inválido.',
  'Internal server error': 'Erro interno do servidor.',
  'Refresh token not found': 'Sessão não encontrada.',
  'You are not authorized to invalidate this session.': 'Não tem permissão para terminar esta sessão.',
  'Missing refresh token.': 'Token de atualização em falta.',
  'cohort already confirmed': 'A turma já foi confirmada.',
  'Credits must be greater than 0': 'Os créditos devem ser superiores a 0.',
  'Credits cannot exceed 30': 'Os créditos não podem exceder 30.',
  'Target year must be greater than 0': 'o ano deve ser superior a 0.',
  'Target semester must be 1 or 2': 'O semestre deve ser 1 ou 2.',
  'One or more student ids not found': 'Um ou mais IDs de estudantes não encontrados.',
  'one or more teacher ids not found': 'Um ou mais IDs de professores não encontrados.',
}

/**
 * Translates backend error messages from English to Portuguese.
 * @param message The original error message from the backend.
 * @returns The translated message or the original if no translation is found.
 */
export function translateErrorMessage(message: string): string {
  if (!message) return 'Ocorreu um erro inesperado.'

  // Check for exact matches
  if (errorTranslations[message]) {
    return errorTranslations[message]
  }

  const lowerMessage = message.toLowerCase()

  // Dynamic matches for "not found"
  if (lowerMessage.includes('not found')) {
    if (lowerMessage.includes('one or more')) return 'Um ou mais recursos não foram encontrados.'
    if (lowerMessage.includes('cohort')) return 'Turma não encontrada.'
    if (lowerMessage.includes('room')) return 'Sala não encontrada.'
    if (lowerMessage.includes('course')) return 'Curso não encontrado.'
    if (lowerMessage.includes('subject')) return 'Disciplina não encontrada.'
    if (lowerMessage.includes('user')) return 'Utilizador não encontrado.'
    if (lowerMessage.includes('timeslot')) return 'Horário não encontrado.'
    if (lowerMessage.includes('timetable')) return 'Horário não encontrado.'
    return 'Recurso não encontrado.'
  }

  // Dynamic matches for "already exists"
  if (lowerMessage.includes('already exists')) {
    if (lowerMessage.includes('cohort')) return 'Esta turma já existe.'
    if (lowerMessage.includes('room')) return 'Esta sala já existe.'
    if (lowerMessage.includes('course')) return 'Este curso já existe.'
    if (lowerMessage.includes('subject')) return 'Esta disciplina já existe.'
    return 'Este recurso já existe.'
  }

  if (lowerMessage.includes('is not a teacher')) {
    return 'O utilizador não é um professor.'
  }

  if (lowerMessage.includes('has reached its maximum capacity')) {
    return 'A turma atingiu a sua capacidade máxima.'
  }

  // Validation errors: Field 'email' must not be empty
  if (message.includes("Field '")) {
    return message
      .split('; ')
      .map((part) => {
        return part
          .replace("Field '", "O campo '")
          .replace("' must not be empty", "' não pode estar vazio")
          .replace("' must not be null", "' não pode ser nulo")
          .replace("' is required", "' é obrigatório")
          .replace("' must be a valid email", "' deve ser um email válido")
          .replace("' must be in the future", "' deve ser uma data futura")
          .replace("' size must be between", "' o tamanho deve estar entre")
          .replace("must not be empty", "não pode estar vazio")
          .replace("must not be null", "não pode ser nulo")
      })
      .join('; ')
  }

  return message
}