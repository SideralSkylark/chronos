<template>
  <div>
    <!-- Header -->
    <PageHeader
      :icon="CalendarDays"
      title="Horários"
      subtitle="Visualizar e gerir horários da instituição"
    >
      <template #actions>
        <div
          v-if="timetableStore.loading"
          class="h-8 flex items-center gap-1.5 px-3 bg-gray-50 border border-gray-100 rounded-md text-xs text-gray-400"
        >
          <Loader2 class="w-3.5 h-3.5 animate-spin" />
          A carregar...
        </div>

        <div
          v-if="timetableStore.solution && !timetableStore.loading"
          class="h-8 flex items-center gap-1.5 px-3 rounded-md text-xs font-medium border"
          :class="
            timetableStore.solution.feasible
              ? 'bg-green-50 text-green-700 border-green-100'
              : 'bg-red-50 text-red-700 border-red-100'
          "
        >
          <CheckCircle v-if="timetableStore.solution.feasible" class="w-3.5 h-3.5" />
          <XCircle v-else class="w-3.5 h-3.5" />
          {{ timetableStore.solution.feasible ? 'Válido' : 'Com conflitos' }}
          <span class="font-mono opacity-40 ml-0.5">{{ timetableStore.solution.score }}</span>
        </div>

        <div
          v-if="timetableStore.solution"
          class="h-8 flex items-center gap-1.5 px-3 rounded-md text-xs font-medium border"
          :class="{
            'bg-amber-50 text-amber-700 border-amber-100': timetableStatus === 'PENDING_APPROVAL',
            'bg-green-50 text-green-700 border-green-100': timetableStatus === 'APPROVED',
            'bg-blue-50  text-blue-700  border-blue-100': timetableStatus === 'PUBLISHED',
            'bg-gray-50  text-gray-500  border-gray-100':
              !timetableStatus || timetableStatus === 'DRAFT',
          }"
        >
          {{
            {
              DRAFT: 'Rascunho',
              PENDING_APPROVAL: 'Aguarda aprovação',
              APPROVED: 'Aprovado',
              REJECTED: 'Rejeitado',
              PUBLISHED: 'Publicado',
            }[timetableStatus ?? 'DRAFT'] ?? 'Rascunho'
          }}
        </div>

        <!-- Divider before action buttons -->
        <div
          v-if="
            timetableStore.solution &&
            (canSubmit || canApprove || canReject || canPublish || isAdmin)
          "
          class="w-px h-5 bg-gray-200"
        />

        <button
          v-if="canSubmit"
          @click="timetableStore.submitForApproval()"
          class="h-8 flex items-center gap-1.5 px-3 bg-amber-500 text-white text-xs font-medium rounded-lg hover:bg-amber-400 transition"
        >
          <Send class="w-3.5 h-3.5" /> Submeter
        </button>
        <button
          v-if="canApprove"
          @click="timetableStore.approve()"
          class="h-8 flex items-center gap-1.5 px-3 bg-green-600 text-white text-xs font-medium rounded-lg hover:bg-green-700 transition"
        >
          <CheckCircle class="w-3.5 h-3.5" /> Aprovar
        </button>
        <button
          v-if="canReject"
          @click="timetableStore.reject()"
          class="h-8 flex items-center gap-1.5 px-3 bg-red-600 text-white text-xs font-medium rounded-lg hover:bg-red-700 transition"
        >
          <XCircle class="w-3.5 h-3.5" /> Rejeitar
        </button>
        <button
          v-if="canPublish"
          @click="timetableStore.publish()"
          class="h-8 flex items-center gap-1.5 px-3 bg-blue-600 text-white text-xs font-medium rounded-lg hover:bg-blue-700 transition"
        >
          <Globe class="w-3.5 h-3.5" /> Publicar
        </button>

        <button
          v-if="canGenerate"
          :disabled="timetableStore.generating || timetableStore.loading"
          @click="showConfirmModal = true"
          class="h-8 flex items-center gap-1.5 px-3 bg-blue-900 text-white text-xs font-medium rounded-lg hover:bg-blue-800 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Loader2 v-if="timetableStore.generating" class="w-3.5 h-3.5 animate-spin" />
          <Zap v-else class="w-3.5 h-3.5" />
          {{ timetableStore.generating ? 'A gerar...' : 'Gerar horário' }}
        </button>
      </template>
    </PageHeader>

    <!-- Filters -->
    <FilterBar :activeFilterCount="0">
      <template #filters>
        <div class="flex flex-col gap-1">
          <label class="text-[10px] font-bold text-blue-800 uppercase tracking-wider"
            >Ano lectivo</label
          >
          <div class="relative">
            <select
              v-model="timetableStore.selectedYear"
              class="h-8 px-3 pr-8 border border-gray-200 rounded-md text-sm text-gray-800 bg-white appearance-none focus:ring-2 focus:ring-blue-100 focus:border-blue-900 outline-none transition cursor-pointer"
            >
              <option v-for="y in availableYears" :key="y" :value="y">
                {{ y }}
              </option>
            </select>
          </div>
        </div>

        <div class="flex flex-col gap-1">
          <label class="text-[10px] font-bold text-blue-800 uppercase tracking-wider"
            >Semestre</label
          >
          <div class="relative">
            <select
              v-model="timetableStore.selectedSemester"
              class="h-8 px-3 pr-8 border border-gray-200 rounded-md text-sm text-gray-800 bg-white appearance-none focus:ring-2 focus:ring-blue-100 focus:border-blue-900 outline-none transition cursor-pointer"
            >
              <option v-for="semester in availableSemesters" :key="semester" :value="semester">
                {{ semester }}º semestre
              </option>
            </select>
          </div>
        </div>

        <div class="flex flex-col gap-1">
          <label class="text-[10px] font-bold text-blue-800 uppercase tracking-wider">Turma</label>
          <div class="relative">
            <select
              v-model="selectedCohort"
              class="h-8 px-3 pr-8 border border-gray-200 rounded-md text-sm text-gray-800 bg-white appearance-none focus:ring-2 focus:ring-blue-100 focus:border-blue-900 outline-none transition cursor-pointer"
            >
              <option value="">Todas as turmas</option>
              <option v-for="c in availableCohorts" :key="c.id" :value="c.id">
                {{ c.displayName }}
              </option>
            </select>
            <ChevronDown
              class="w-3.5 h-3.5 text-gray-400 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none"
            />
          </div>
        </div>
      </template>

      <template #actions>
        <div v-if="hasLessons" class="flex items-center justify-end gap-4 h-8">
          <div v-for="item in yearLegend" :key="item.year" class="flex items-center gap-1.5">
            <div class="w-2.5 h-2.5 rounded-sm" :class="item.dot"></div>
            <span class="text-xs text-gray-400">{{ item.label }}</span>
          </div>
        </div>
      </template>
    </FilterBar>

    <!-- Undo Bar -->
    <Transition name="undo-bar">
      <div
        v-if="undoAction"
        :key="undoAction.label"
        class="w-full flex flex-col bg-white border border-gray-100 shadow-sm rounded-[10px] mb-3 overflow-hidden"
      >
        <div class="flex items-center justify-between px-4 py-2.5">
          <div class="flex items-center gap-2">
            <ArrowRightLeft class="w-3.5 h-3.5 text-blue-900" />
            <span class="text-xs text-gray-600">{{ undoAction.label }}</span>
          </div>
          <button
            @click="triggerUndo"
            class="h-6 flex items-center gap-1 px-2.5 text-xs font-medium text-blue-900 bg-blue-50 border border-blue-100 rounded-md hover:bg-blue-100 transition"
          >
            <ArrowRightLeft class="w-3 h-3" />
            Desfazer
          </button>
        </div>
        <div class="undo-progress"></div>
      </div>
    </Transition>

    <!-- Table + side panel -->
    <div class="flex gap-4 items-start">
      <div
        class="flex-1 bg-white rounded-[10px] shadow-sm border border-gray-100 overflow-hidden min-w-0"
      >
        <div
          v-if="timetableStore.generating"
          class="flex flex-col items-center justify-center py-24 gap-3"
        >
          <Loader2 class="w-7 h-7 animate-spin text-blue-900" />
          <p class="text-sm font-medium text-gray-600">A gerar horário...</p>
          <p class="text-xs text-gray-400">
            {{
              pollElapsed < 60
                ? `${pollElapsed}s decorridos`
                : `${Math.floor(pollElapsed / 60)}m ${pollElapsed % 60}s decorridos`
            }}
          </p>
        </div>

        <div
          v-else-if="timetableStore.loading"
          class="flex flex-col items-center justify-center py-24 gap-3"
        >
          <Loader2 class="w-7 h-7 animate-spin text-blue-900" />
          <p class="text-sm font-medium text-gray-600">A carregar horário...</p>
        </div>

        <div v-else-if="!hasLessons" class="flex flex-col items-center justify-center py-24 gap-3">
          <div class="bg-gray-100 p-4 rounded-full">
            <CalendarDays class="w-8 h-8 text-gray-400" />
          </div>
          <p class="text-sm font-semibold text-gray-600">Nenhum horário gerado</p>
          <p class="text-xs text-gray-400">
            {{
              canGenerate
                ? 'Seleccione o ano e semestre e clique em "Gerar horário" no cabeçalho.'
                : 'Aguarde a geração pelo administrador ou assistente.'
            }}
          </p>
        </div>

        <div v-else class="overflow-x-auto">
          <table class="min-w-full border-collapse">
            <thead>
              <tr>
                <th
                  class="w-24 bg-gray-50 border-b border-r border-gray-100 px-3 py-3 text-xs font-medium text-gray-400 uppercase tracking-wide text-center"
                >
                  Bloco
                </th>
                <th
                  v-for="day in days"
                  :key="day.value"
                  class="bg-gray-50 border-b border-r border-gray-100 px-3 py-3 text-xs font-medium text-gray-500 uppercase tracking-wide text-center last:border-r-0"
                >
                  {{ day.label }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="block in blocks"
                :key="block.id"
                class="border-b border-gray-50 last:border-b-0"
              >
                <td class="border-r border-gray-100 bg-gray-50 px-3 py-3 text-center align-middle">
                  <div class="flex flex-col items-center gap-1">
                    <span class="text-xs font-mono font-semibold text-gray-600">{{
                      block.startTime
                    }}</span>
                    <span
                      class="text-xs font-medium px-2 py-0.5 rounded-full"
                      :class="
                        block.period === 'MORNING'
                          ? 'bg-amber-50 text-amber-600'
                          : 'bg-blue-50 text-blue-600'
                      "
                    >
                      {{ block.period === 'MORNING' ? 'Manhã' : 'Tarde' }}
                    </span>
                    <span class="text-xs font-mono text-gray-400">{{ block.endTime }}</span>
                  </div>
                </td>
                <td
                  v-for="day in days"
                  :key="`${block.id}-${day.value}`"
                  class="border-r border-gray-100 last:border-r-0 p-1.5 align-top transition-colors"
                  :class="cellClass(day.value, block.startTime)"
                  style="min-width: 130px; min-height: 76px"
                  @click="handleCellClick(day.value, block.startTime)"
                >
                  <div
                    v-if="hasEmptySlot(day.value, block.startTime)"
                    class="h-full flex items-center justify-center py-4"
                  >
                    <div class="w-7 h-7 rounded-full bg-green-200 flex items-center justify-center">
                      <ArrowRightLeft class="w-3.5 h-3.5 text-green-700" />
                    </div>
                  </div>
                  <div
                    v-for="lesson in getCellLessons(day.value, block.startTime)"
                    :key="lesson.id"
                    class="rounded-md px-2 py-1.5 mb-1 transition-all relative text-xs"
                    :class="[
                      yearColorClass(lesson.cohort.year),
                      selectedLesson?.id === lesson.id
                        ? 'ring-2 ring-blue-500 shadow-md scale-[1.02]'
                        : canSelectLesson
                          ? 'cursor-pointer hover:shadow-sm hover:scale-[1.02]'
                          : 'cursor-default hover:shadow-sm',
                    ]"
                    @click.stop="canSelectLesson && selectLesson(lesson)"
                    @mouseenter="!selectedLesson && (hoveredLesson = lesson)"
                    @mouseleave="!selectedLesson && (hoveredLesson = null)"
                  >
                    <p class="font-semibold leading-tight line-clamp-2">
                      {{ lesson.subject.name }}
                    </p>
                    <p class="opacity-60 mt-0.5">{{ lesson.cohort.displayName }}</p>
                    <div
                      v-if="hasSwapSlot(day.value, block.startTime)"
                      class="absolute top-1 right-1"
                    >
                      <div
                        class="w-4 h-4 rounded-full bg-orange-400 flex items-center justify-center shadow"
                      >
                        <ArrowRightLeft class="w-2.5 h-2.5 text-white" />
                      </div>
                    </div>
                    <div v-if="isPhantomTeacher(lesson)" class="absolute top-1 left-1">
                      <div class="w-2 h-2 rounded-full bg-red-400 ring-1 ring-white"></div>
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Side Panel Section -->
      <div v-if="selectedCohort" class="flex-shrink-0" style="width: 264px">
        <div
          v-if="canEdit && selectedCohort && !selectedLesson"
          class="bg-white rounded-[10px] shadow-sm border border-gray-100 flex flex-col items-center justify-center gap-2 px-4 py-6"
        >
          <div class="bg-blue-50 rounded-full p-2.5">
            <ArrowRightLeft class="w-3.5 h-3.5 text-blue-900" />
          </div>
          <p class="text-xs text-gray-400 text-center leading-relaxed">
            Seleccione uma aula da turma para ver as permutações disponíveis
          </p>
        </div>

        <!-- Swap side panel -->
        <Transition name="slide-panel">
          <div
            v-if="canEdit && selectedCohort && selectedLesson"
            class="bg-white rounded-[10px] shadow-sm border border-gray-100 w-full"
          >
            <div class="p-4 border-b border-gray-100 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <ArrowRightLeft class="w-4 h-4 text-blue-900" />
                <h3 class="text-sm font-semibold text-gray-900">Permutar aula</h3>
              </div>
              <button
                @click="clearSelection"
                class="text-gray-400 hover:text-gray-600 transition p-0.5"
              >
                <X class="w-4 h-4" />
              </button>
            </div>
            <div class="p-4 space-y-3">
              <div class="bg-gray-50 rounded-md p-3 space-y-0.5">
                <p class="font-semibold text-gray-800 text-sm">{{ selectedLesson.subject.name }}</p>
                <p class="text-xs text-gray-400">{{ selectedLesson.cohort.displayName }}</p>
                <p class="text-xs text-gray-400">
                  {{ dayLabel(selectedLesson.timeslot?.dayOfWeek) }} ·
                  {{ selectedLesson.timeslot?.startTime?.substring(0, 5) }}
                </p>
                <p class="text-xs text-gray-400">{{ selectedLesson.room?.name }}</p>
              </div>

              <PhantomTeacherPanel
                v-if="isPhantomTeacher(selectedLesson)"
                :lesson="selectedLesson"
                @replaced="onPhantomReplaced"
              />

              <button
                :disabled="loadingSlots"
                @click="calculateValidSlots"
                class="w-full h-8 flex items-center justify-center gap-1.5 px-3 bg-blue-900 text-white text-xs font-medium rounded-lg hover:bg-blue-800 transition disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Loader2 v-if="loadingSlots" class="w-3.5 h-3.5 animate-spin" />
                <ArrowRightLeft v-else class="w-3.5 h-3.5" />
                {{
                  loadingSlots
                    ? 'A calcular...'
                    : slotsCalculated
                      ? 'Recalcular'
                      : 'Calcular permutações'
                }}
              </button>

              <template v-if="slotsCalculated">
                <p v-if="validSlots.length > 0" class="text-xs text-gray-400 text-center">
                  {{ validSlots.length }} permutação(ões) disponível(eis)
                </p>
                <p
                  v-else
                  class="text-xs text-amber-600 bg-amber-50 border border-amber-100 rounded-md p-2.5 text-center"
                >
                  Nenhuma permutação válida.
                </p>
              </template>

              <div class="border border-gray-100 rounded-md overflow-hidden">
                <div
                  class="flex items-center justify-between px-3 py-2 bg-gray-50 border-b border-gray-100"
                >
                  <span class="text-[10px] font-bold text-blue-800 uppercase tracking-wider">
                    Trocar com aula da mesma turma
                  </span>
                </div>

                <div class="p-2.5 space-y-2">
                  <button
                    :disabled="loadingCohortSwaps"
                    @click="calculateCohortSwaps"
                    class="w-full h-8 flex items-center justify-center gap-1.5 px-3 bg-gray-700 text-white text-xs font-medium rounded-lg hover:bg-gray-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Loader2 v-if="loadingCohortSwaps" class="w-3.5 h-3.5 animate-spin" />
                    <ArrowRightLeft v-else class="w-3.5 h-3.5" />
                    {{
                      loadingCohortSwaps
                        ? 'A calcular...'
                        : cohortSwapsCalculated
                          ? 'Recalcular'
                          : 'Ver trocas'
                    }}
                  </button>
                  <template v-if="cohortSwapsCalculated">
                    <div
                      v-if="cohortSwapCandidates.length > 0"
                      class="space-y-1 max-h-44 overflow-y-auto"
                    >
                      <button
                        v-for="c in cohortSwapCandidates"
                        :key="c.scheduledClassId"
                        @click="pendingCohortSwap = c"
                        class="w-full text-left px-3 py-2 rounded-lg bg-gray-50 hover:bg-gray-100 transition text-xs border border-gray-100"
                      >
                        <p class="font-semibold text-gray-800 truncate">{{ c.subjectName }}</p>
                        <p class="text-gray-400 mt-0.5">
                          {{ dayLabel(c.dayOfWeek) }} · {{ c.startTime.substring(0, 5) }} ·
                          {{ c.roomName }}
                        </p>
                      </button>
                    </div>
                    <p
                      v-else
                      class="text-xs text-amber-600 bg-amber-50 border border-amber-100 rounded-lg p-2.5 text-center"
                    >
                      Nenhuma troca válida.
                    </p>
                  </template>
                </div>
              </div>

              <button
                @click="clearSelection"
                class="w-full h-8 flex items-center justify-center text-xs text-gray-400 border border-gray-200 rounded-lg hover:bg-gray-50 transition"
              >
                Cancelar
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <!-- Hover tooltip -->

    <Teleport to="body">
      <Transition name="tooltip">
        <div
          v-if="hoveredLesson && !selectedLesson"
          class="fixed bottom-6 right-6 bg-gray-900 text-white rounded-[10px] shadow-2xl p-4 w-52 z-50 pointer-events-none"
        >
          <p class="font-semibold text-sm mb-3 pb-2 border-b border-white/10 leading-snug">
            {{ hoveredLesson.subject.name }}
          </p>

          <div class="space-y-1.5">
            <div class="flex justify-between text-xs gap-3">
              <span class="text-gray-400 shrink-0">Turma</span
              ><span class="text-right">{{ hoveredLesson.cohort.displayName }}</span>
            </div>

            <div class="flex justify-between text-xs gap-3">
              <span class="text-gray-400 shrink-0">Professor</span
              ><span class="text-right">{{ hoveredLesson.teacher?.fullName ?? '—' }}</span>
            </div>

            <div class="flex justify-between text-xs gap-3">
              <span class="text-gray-400 shrink-0">Sala</span
              ><span class="text-right">{{ hoveredLesson.room?.name ?? '—' }}</span>
            </div>

            <div class="flex justify-between text-xs gap-3">
              <span class="text-gray-400 shrink-0">Créditos</span
              ><span>{{ hoveredLesson.subject.credits }}</span>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Modal: Confirm generate -->

    <div
      v-if="showConfirmModal"
      class="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50"
      @click.self="showConfirmModal = false"
    >
      <div class="bg-white rounded-[10px] shadow-2xl w-full max-w-sm border border-gray-100">
        <div class="p-5 border-b border-gray-100 flex items-center gap-3">
          <div class="bg-blue-50 p-2 rounded-md">
            <Zap class="w-4 h-4 text-blue-900" />
          </div>

          <h2 class="text-base font-semibold text-gray-900">Gerar horário</h2>
        </div>

        <div class="p-5">
          <p class="text-sm text-gray-500 leading-relaxed">
            Vai gerar o horário para
            <strong class="text-gray-700"
              >{{ selectedYear }} · {{ selectedSemester }}º semestre</strong
            >.

            <template v-if="timetableStore.solution"
              ><br /><span class="text-amber-600 font-medium"
                >O horário existente será substituído.</span
              ></template
            >

            A operação pode demorar até 5 minutos.
          </p>

          <div class="flex gap-2 mt-5">
            <button
              @click="showConfirmModal = false"
              class="flex-1 h-9 border border-gray-200 rounded-lg text-sm text-gray-500 hover:bg-gray-50 transition flex items-center justify-center gap-1.5"
            >
              <X class="w-3.5 h-3.5" /> Cancelar
            </button>

            <button
              @click="handleGenerate"
              class="flex-1 h-9 bg-blue-900 text-white rounded-lg text-sm font-medium hover:bg-blue-800 transition flex items-center justify-center gap-1.5"
            >
              <Zap class="w-3.5 h-3.5" /> Confirmar
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal: Move / Swap -->

    <div
      v-if="pendingSwap"
      class="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50"
      @click.self="pendingSwap = null"
    >
      <div class="bg-white rounded-[10px] shadow-2xl w-full max-w-sm border border-gray-100">
        <div class="p-5 border-b border-gray-100 flex items-center gap-3">
          <div class="p-2 rounded-md" :class="pendingSwap.isSwap ? 'bg-orange-50' : 'bg-green-50'">
            <ArrowRightLeft v-if="pendingSwap.isSwap" class="w-4 h-4 text-orange-600" />

            <ArrowRight v-else class="w-4 h-4 text-green-600" />
          </div>

          <h2 class="text-base font-semibold text-gray-900">
            {{ pendingSwap.isSwap ? 'Trocar aulas' : 'Mover aula' }}
          </h2>
        </div>

        <div class="p-5 space-y-3">
          <div class="bg-blue-50 border border-blue-100 rounded-md p-3 text-xs">
            <p class="text-[10px] font-bold text-blue-800 uppercase tracking-wide mb-1">A mover</p>

            <p class="font-semibold text-gray-800">{{ selectedLesson?.subject.name }}</p>

            <div class="flex items-center gap-1.5 mt-2 flex-wrap">
              <span
                class="text-xs px-2 py-0.5 rounded-md bg-gray-100 text-gray-500 border border-gray-200 font-medium"
              >
                {{ dayLabel(selectedLesson?.timeslot?.dayOfWeek) }} ·
                {{ selectedLesson?.timeslot?.startTime?.substring(0, 5) }}
              </span>

              <ArrowRight class="w-3 h-3 text-gray-400 flex-shrink-0" />

              <span
                class="text-xs px-2 py-0.5 rounded-md bg-blue-50 text-blue-700 border border-blue-200 font-medium"
              >
                {{ dayLabel(pendingSwap.dayOfWeek) }} ·
                {{ pendingSwap.startTime.substring(0, 5) }} · {{ pendingSwap.roomName }}
              </span>
            </div>
          </div>

          <div
            v-if="pendingSwap.isSwap"
            class="bg-orange-50 border border-orange-100 rounded-md p-3 text-xs"
          >
            <p class="text-[10px] font-bold text-blue-800 uppercase tracking-wide mb-1">
              Deslocada
            </p>

            <p class="font-semibold text-gray-800">{{ pendingSwap.swapWithSubject }}</p>

            <div class="flex items-center gap-1.5 mt-2 flex-wrap">
              <span
                class="text-xs px-2 py-0.5 rounded-md bg-gray-100 text-gray-500 border border-gray-200 font-medium"
              >
                {{ dayLabel(pendingSwap.dayOfWeek) }} · {{ pendingSwap.startTime.substring(0, 5) }}
              </span>

              <ArrowRight class="w-3 h-3 text-gray-400 flex-shrink-0" />

              <span
                class="text-xs px-2 py-0.5 rounded-md bg-orange-50 text-orange-700 border border-orange-200 font-medium"
              >
                {{ dayLabel(selectedLesson?.timeslot?.dayOfWeek) }} ·
                {{ selectedLesson?.timeslot?.startTime?.substring(0, 5) }} ·
                {{ selectedLesson?.room?.name ?? '—' }}
              </span>
            </div>
          </div>

          <div class="flex gap-2 pt-1">
            <button
              @click="pendingSwap = null"
              class="flex-1 h-9 border border-gray-200 rounded-lg text-sm text-gray-500 hover:bg-gray-50 transition flex items-center justify-center gap-1.5"
            >
              <X class="w-3.5 h-3.5" /> Cancelar
            </button>

            <button
              @click="handleApplySwap"
              :disabled="applyingSwap"
              class="flex-1 h-9 text-white rounded-lg text-sm font-medium transition flex items-center justify-center gap-1.5 disabled:opacity-50"
              :class="
                pendingSwap.isSwap
                  ? 'bg-orange-500 hover:bg-orange-600'
                  : 'bg-green-600 hover:bg-green-700'
              "
            >
              <Loader2 v-if="applyingSwap" class="w-3.5 h-3.5 animate-spin" />

              <ArrowRightLeft v-else class="w-3.5 h-3.5" />

              Confirmar
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal: Cohort swap -->

    <div
      v-if="pendingCohortSwap"
      class="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50"
      @click.self="pendingCohortSwap = null"
    >
      <div class="bg-white rounded-[10px] shadow-2xl w-full max-w-sm border border-gray-100">
        <div class="p-5 border-b border-gray-100 flex items-center gap-3">
          <div class="bg-gray-100 p-2 rounded-md">
            <ArrowRightLeft class="w-4 h-4 text-gray-600" />
          </div>

          <h2 class="text-base font-semibold text-gray-900">Trocar aulas da turma</h2>
        </div>

        <div class="p-5 space-y-3">
          <div class="bg-blue-50 border border-blue-100 rounded-md p-3 text-xs">
            <p class="text-[10px] font-bold text-blue-800 uppercase tracking-wide mb-1">Aula A</p>

            <p class="font-semibold text-gray-800">{{ selectedLesson?.subject.name }}</p>

            <div class="flex items-center gap-1.5 mt-2 flex-wrap">
              <span
                class="text-xs px-2 py-0.5 rounded-md bg-gray-100 text-gray-500 border border-gray-200 font-medium"
              >
                {{ dayLabel(selectedLesson?.timeslot?.dayOfWeek) }} ·
                {{ selectedLesson?.timeslot?.startTime?.substring(0, 5) }}
              </span>

              <ArrowRight class="w-3 h-3 text-gray-400 flex-shrink-0" />

              <span
                class="text-xs px-2 py-0.5 rounded-md bg-blue-50 text-blue-700 border border-blue-200 font-medium"
              >
                {{ dayLabel(pendingCohortSwap.dayOfWeek) }} ·
                {{ pendingCohortSwap.startTime.substring(0, 5) }} · {{ pendingCohortSwap.roomName }}
              </span>
            </div>
          </div>

          <div class="bg-gray-50 border border-gray-100 rounded-md p-3 text-xs">
            <p class="text-[10px] font-bold text-blue-800 uppercase tracking-wide mb-1">Aula B</p>

            <p class="font-semibold text-gray-800">{{ pendingCohortSwap.subjectName }}</p>

            <div class="flex items-center gap-1.5 mt-2 flex-wrap">
              <span
                class="text-xs px-2 py-0.5 rounded-md bg-gray-100 text-gray-500 border border-gray-200 font-medium"
              >
                {{ dayLabel(pendingCohortSwap.dayOfWeek) }} ·
                {{ pendingCohortSwap.startTime.substring(0, 5) }}
              </span>

              <ArrowRight class="w-3 h-3 text-gray-400 flex-shrink-0" />

              <span
                class="text-xs px-2 py-0.5 rounded-md bg-gray-100 text-gray-600 border border-gray-200 font-medium"
              >
                {{ dayLabel(selectedLesson?.timeslot?.dayOfWeek) }} ·
                {{ selectedLesson?.timeslot?.startTime?.substring(0, 5) }} ·
                {{ selectedLesson?.room?.name ?? '—' }}
              </span>
            </div>
          </div>

          <div class="flex gap-2 pt-1">
            <button
              @click="pendingCohortSwap = null"
              class="flex-1 h-9 border border-gray-200 rounded-lg text-sm text-gray-500 hover:bg-gray-50 transition flex items-center justify-center gap-1.5"
            >
              <X class="w-3.5 h-3.5" /> Cancelar
            </button>

            <button
              @click="handleApplyCohortSwap"
              :disabled="applyingCohortSwap"
              class="flex-1 h-9 bg-gray-700 text-white rounded-lg text-sm font-medium hover:bg-gray-600 transition flex items-center justify-center gap-1.5 disabled:opacity-50"
            >
              <Loader2 v-if="applyingCohortSwap" class="w-3.5 h-3.5 animate-spin" />

              <ArrowRightLeft v-else class="w-3.5 h-3.5" />

              Confirmar
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTimetableStore } from '@/stores/timetable'
import { useCourseStore } from '@/stores/course'
import { useToast } from '@/composables/useToast'
import {
  permutationService,
  type ValidSlot,
  type CohortSwapCandidate,
} from '@/services/permutationService'
import type { LessonAssignment, CohortInfo } from '@/services/dto/timetable'
import PageHeader from '@/components/ui/PageHeader.vue'
import FilterBar from '@/components/ui/FilterBar.vue'
import PhantomTeacherPanel from '@/components/ui/PhantomTeacherPanel.vue'
import {
  CalendarDays,
  ChevronDown,
  Zap,
  Loader2,
  CheckCircle,
  XCircle,
  ArrowRightLeft,
  ArrowRight,
  X,
  Send,
  Globe,
} from 'lucide-vue-next'

const auth = useAuthStore()
const timetableStore = useTimetableStore()
const courseStore = useCourseStore()
const toast = useToast()

const isAdmin = computed(() => auth.user?.roles?.includes('ADMIN') ?? false)
const isDirector = computed(() => auth.user?.roles?.includes('DIRECTOR') ?? false)
const isAssistant = computed(() => auth.user?.roles?.includes('ASISTENT') ?? false)
const isCoordinator = computed(() => auth.user?.roles?.includes('COORDINATOR') ?? false)

const canGenerate = computed(() => isAdmin.value || isAssistant.value)

const canEdit = computed(() => {
  if (isAdmin.value || isDirector.value || isAssistant.value) return true
  if (isCoordinator.value && selectedCohort.value) {
    const cohort = availableCohorts.value.find((c) => c.id === selectedCohort.value)
    if (cohort) {
      const course = courseStore.courses.find((c) => c.id === cohort.courseId)
      return course?.coordinatorId === auth.user?.id
    }
  }
  return false
})

const availableYears = computed(() => timetableStore.availablePeriods.map((p) => p.year))

const availableSemesters = computed(() => timetableStore.availableSemesters)
const selectedYear = computed({
  get: () => timetableStore.selectedYear,
  set: (value) => {
    if (value != null && timetableStore.selectedSemester != null) {
      timetableStore.setPeriod(value, timetableStore.selectedSemester)
    }
  },
})
const selectedSemester = computed({
  get: () => timetableStore.selectedSemester,
  set: (value) => {
    if (value != null && timetableStore.selectedYear != null) {
      timetableStore.setPeriod(timetableStore.selectedYear, value)
    }
  },
})
const selectedCohort = ref<number | ''>('')
const showConfirmModal = ref(false)
const hoveredLesson = ref<LessonAssignment | null>(null)
const pollAttempt = ref(0)
const pollElapsed = ref(0)

const selectedLesson = ref<LessonAssignment | null>(null)
const validSlots = ref<ValidSlot[]>([])
const slotsCalculated = ref(false)
const loadingSlots = ref(false)
const pendingSwap = ref<ValidSlot | null>(null)
const applyingSwap = ref(false)

const cohortSwapCandidates = ref<CohortSwapCandidate[]>([])
const loadingCohortSwaps = ref(false)
const cohortSwapsCalculated = ref(false)
const pendingCohortSwap = ref<CohortSwapCandidate | null>(null)
const applyingCohortSwap = ref(false)

const undoAction = ref<null | { label: string; fn: () => Promise<void> }>(null)
let undoTimer: ReturnType<typeof setTimeout> | null = null

function showUndo(label: string, fn: () => Promise<void>) {
  undoAction.value = { label, fn }
  if (undoTimer) clearTimeout(undoTimer)
  undoTimer = setTimeout(() => {
    undoAction.value = null
  }, 6000)
}

async function triggerUndo() {
  if (!undoAction.value) return
  const action = undoAction.value
  try {
    await action.fn()
    toast.success('Acção desfeita!')
  } catch {
    toast.error('Erro ao desfazer acção.')
  } finally {
    if (undoTimer) clearTimeout(undoTimer)
    undoAction.value = null
  }
}

const timetableStatus = computed(() => timetableStore.solution?.status)
const canSubmit = computed(
  () => (isAdmin.value || isAssistant.value) && timetableStatus.value === 'DRAFT',
)
const canApprove = computed(
  () => (isAdmin.value || isDirector.value) && timetableStatus.value === 'PENDING_APPROVAL',
)
const canReject = computed(
  () => (isAdmin.value || isDirector.value) && timetableStatus.value === 'PENDING_APPROVAL',
)
const canPublish = computed(
  () => (isAdmin.value || isDirector.value) && timetableStatus.value === 'APPROVED',
)

const days = [
  { value: 'MONDAY', label: 'Segunda' },
  { value: 'TUESDAY', label: 'Terça' },
  { value: 'WEDNESDAY', label: 'Quarta' },
  { value: 'THURSDAY', label: 'Quinta' },
  { value: 'FRIDAY', label: 'Sexta' },
]
const blocks = [
  { id: 1, startTime: '07:00', endTime: '08:45', period: 'MORNING' },
  { id: 2, startTime: '08:50', endTime: '10:35', period: 'MORNING' },
  { id: 3, startTime: '10:40', endTime: '12:25', period: 'MORNING' },
  { id: 4, startTime: '12:30', endTime: '14:15', period: 'AFTERNOON' },
  { id: 5, startTime: '14:20', endTime: '16:05', period: 'AFTERNOON' },
  { id: 6, startTime: '16:10', endTime: '17:55', period: 'AFTERNOON' },
]
const yearLegend = [
  { year: 1, label: '1º Ano', dot: 'bg-green-300' },
  { year: 2, label: '2º Ano', dot: 'bg-blue-300' },
  { year: 3, label: '3º Ano', dot: 'bg-orange-300' },
  { year: 4, label: '4º Ano', dot: 'bg-purple-300' },
  { year: 5, label: '5º Ano', dot: 'bg-pink-300' },
]

const allLessons = computed(() => timetableStore.solution?.lessonAssignments ?? [])
const availableCohorts = computed(() => {
  const seen = new Set<number>()
  const result: CohortInfo[] = []
  for (const l of allLessons.value) {
    if (!seen.has(l.cohort.id)) {
      seen.add(l.cohort.id)
      result.push(l.cohort)
    }
  }
  return result.sort((a, b) => a.displayName.localeCompare(b.displayName))
})
const filteredLessons = computed(() =>
  selectedCohort.value
    ? allLessons.value.filter((l) => l.cohort.id === selectedCohort.value)
    : allLessons.value,
)
const hasLessons = computed(() => filteredLessons.value.length > 0)

const validSlotMap = computed(() => {
  const map = new Map<string, ValidSlot[]>()
  for (const s of validSlots.value) {
    const key = `${s.dayOfWeek}|${s.startTime.substring(0, 5)}`
    const existing = map.get(key)
    if (existing) existing.push(s)
    else map.set(key, [s])
  }
  return map
})

function getSlotsForCell(day: string, blockStart: string): ValidSlot[] {
  return validSlotMap.value.get(`${day}|${blockStart}`) ?? []
}
function hasEmptySlot(day: string, blockStart: string) {
  return getSlotsForCell(day, blockStart).some((s) => !s.isSwap)
}
function hasSwapSlot(day: string, blockStart: string) {
  const s = getSlotsForCell(day, blockStart)
  return s.length > 0 && s.every((s) => s.isSwap)
}
function cellClass(day: string, blockStart: string) {
  const s = getSlotsForCell(day, blockStart)
  if (!s.length) return ''
  return s.some((s) => !s.isSwap)
    ? 'bg-green-50 ring-1 ring-inset ring-green-200 cursor-pointer'
    : 'bg-orange-50 ring-1 ring-inset ring-orange-200 cursor-pointer'
}
function handleCellClick(day: string, blockStart: string) {
  const s = getSlotsForCell(day, blockStart)
  if (!s.length) return
  pendingSwap.value = s.find((s) => !s.isSwap) ?? (s[0] as ValidSlot)
}
function getCellLessons(day: string, blockStart: string) {
  return filteredLessons.value.filter(
    (l) =>
      l.timeslot?.dayOfWeek === day && (l.timeslot?.startTime ?? '').substring(0, 5) === blockStart,
  )
}
function yearColorClass(year: number): string {
  return (
    (
      {
        1: 'bg-green-100 text-green-800 border border-green-200',
        2: 'bg-blue-100 text-blue-800 border border-blue-200',
        3: 'bg-orange-100 text-orange-800 border border-orange-200',
        4: 'bg-purple-100 text-purple-800 border border-purple-200',
        5: 'bg-pink-100 text-pink-800 border border-pink-200',
      } as Record<number, string>
    )[year] ?? 'bg-gray-100 text-gray-800 border border-gray-200'
  )
}
function dayLabel(day?: string) {
  return days.find((d) => d.value === day)?.label ?? day ?? ''
}

function isPhantomTeacher(lesson: LessonAssignment): boolean {
  return (lesson.teacher?.fullName ?? '').toUpperCase().includes('PHANTOM')
}

const canSelectLesson = computed(() => canEdit.value && !!selectedCohort.value)

function selectLesson(lesson: LessonAssignment) {
  if (selectedLesson.value?.id === lesson.id) {
    clearSelection()
    return
  }
  selectedLesson.value = lesson
  validSlots.value = []
  slotsCalculated.value = false
}

function clearSelection() {
  selectedLesson.value = null
  validSlots.value = []
  slotsCalculated.value = false
  cohortSwapCandidates.value = []
  cohortSwapsCalculated.value = false
  hoveredLesson.value = null
}

onMounted(async () => {
  await timetableStore.initialize()
  await courseStore.fetchAllCoursesSimple()
})
watch([() => timetableStore.selectedYear, () => timetableStore.selectedSemester], () => {
  selectedCohort.value = ''
  clearSelection()
})

function onYearChange(year: number) {
  timetableStore.setPeriod(year, timetableStore.selectedSemester ?? 1)
}

function onSemesterChange(semester: number) {
  timetableStore.setPeriod(timetableStore.selectedYear ?? new Date().getFullYear(), semester)
}

async function calculateValidSlots() {
  if (!selectedLesson.value) return
  loadingSlots.value = true
  slotsCalculated.value = false
  try {
    validSlots.value = await permutationService.getValidSlots(
      selectedLesson.value.id,
      timetableStore.selectedYear!,
      timetableStore.selectedSemester!,
    )
    slotsCalculated.value = true
    if (!validSlots.value.length) toast.info('Nenhuma permutação válida encontrada.')
  } catch {
    toast.error('Erro ao calcular permutações.')
  } finally {
    loadingSlots.value = false
  }
}
async function calculateCohortSwaps() {
  if (!selectedLesson.value) return
  loadingCohortSwaps.value = true
  cohortSwapsCalculated.value = false
  try {
    cohortSwapCandidates.value = await permutationService.getCohortSwapCandidates(
      selectedLesson.value.id,
      timetableStore.selectedYear!,
      timetableStore.selectedSemester!,
    )
    cohortSwapsCalculated.value = true
    if (!cohortSwapCandidates.value.length) toast.info('Nenhuma troca válida encontrada.')
  } catch {
    toast.error('Erro ao calcular trocas.')
  } finally {
    loadingCohortSwaps.value = false
  }
}
async function handleApplySwap() {
  if (!selectedLesson.value || !pendingSwap.value) return
  applyingSwap.value = true

  const originalTimeslotId = selectedLesson.value.timeslot?.id
  const originalRoomId = selectedLesson.value.room?.id
  const lessonId = selectedLesson.value.id
  const isSwap = pendingSwap.value.isSwap

  try {
    await permutationService.applySwap(
      lessonId,
      pendingSwap.value.timeslotId,
      pendingSwap.value.roomId,
      pendingSwap.value.swapWithId,
    )
    toast.success(isSwap ? 'Aulas trocadas com sucesso!' : 'Aula movida com sucesso!')

    showUndo(isSwap ? 'Troca de aulas efectuada' : 'Aula movida com sucesso', async () => {
      if (originalTimeslotId !== undefined && originalRoomId !== undefined) {
        await permutationService.applySwap(lessonId, originalTimeslotId, originalRoomId, null)
        await timetableStore.loadForPeriod(
          timetableStore.selectedYear!,
          timetableStore.selectedSemester!,
        )
      }
    })

    pendingSwap.value = null
    clearSelection()
    await timetableStore.loadForPeriod(
      timetableStore.selectedYear!,
      timetableStore.selectedSemester!,
    )
  } catch {
    toast.error('Erro ao aplicar permutação.')
  } finally {
    applyingSwap.value = false
  }
}
async function handleApplyCohortSwap() {
  if (!selectedLesson.value || !pendingCohortSwap.value) return
  applyingCohortSwap.value = true

  const lessonAId = selectedLesson.value.id
  const lessonBId = pendingCohortSwap.value.scheduledClassId

  try {
    await permutationService.applyCohortSwap(lessonAId, lessonBId)
    toast.success('Aulas trocadas com sucesso!')

    showUndo('Troca de aulas efectuada', async () => {
      await permutationService.applyCohortSwap(lessonAId, lessonBId)
      await timetableStore.loadForPeriod(
        timetableStore.selectedYear!,
        timetableStore.selectedSemester!,
      )
    })

    pendingCohortSwap.value = null
    clearSelection()
    await timetableStore.loadForPeriod(
      timetableStore.selectedYear!,
      timetableStore.selectedSemester!,
    )
  } catch {
    toast.error('Erro ao aplicar troca.')
  } finally {
    applyingCohortSwap.value = false
  }
}
async function handleGenerate() {
  showConfirmModal.value = false
  selectedCohort.value = ''
  clearSelection()

  pollAttempt.value = 0
  pollElapsed.value = 0

  toast.info('A gerar horário, pode demorar até 5 minutos...')

  try {
    await timetableStore.generate(
      timetableStore.selectedYear!,
      timetableStore.selectedSemester!,
      (attempt, elapsed) => {
        pollAttempt.value = attempt
        pollElapsed.value = elapsed
      },
    )

    toast.success('Horário gerado com sucesso!')
  } catch {
    toast.error(timetableStore.error ?? 'Erro ao gerar horário.')
  }
}

async function onPhantomReplaced() {
  clearSelection()
  await timetableStore.loadForPeriod(timetableStore.selectedYear!, timetableStore.selectedSemester!)
}
</script>

<style scoped>
.tooltip-enter-active,
.tooltip-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.tooltip-enter-from,
.tooltip-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.slide-panel-enter-active,
.slide-panel-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.slide-panel-enter-from,
.slide-panel-leave-to {
  opacity: 0;
  transform: translateX(16px);
}

.undo-bar-enter-active,
.undo-bar-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}
.undo-bar-enter-from,
.undo-bar-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@keyframes undo-shrink {
  from {
    width: 100%;
  }
  to {
    width: 0%;
  }
}
.undo-progress {
  height: 3px;
  background: #bfdbfe;
  border-radius: 0 0 10px 10px;
  animation: undo-shrink 6s linear forwards;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
