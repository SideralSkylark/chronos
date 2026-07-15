package com.timetable.timetable.scheduler_engine.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import com.timetable.timetable.scheduler_engine.domain.LessonAssignment;
import com.timetable.timetable.scheduler_engine.domain.TimetableSolution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Timefold Solver.
 *
 * <p>The solver works in two phases: 1. Construction Heuristic: Quickly builds an initial solution
 * 2. Local Search: Improves the solution iteratively
 */
@Configuration
public class TimefoldConfig {

  @Bean
  public SolverConfig solverConfig() {
    return new SolverConfig()
        .withSolutionClass(TimetableSolution.class)
        .withEntityClasses(LessonAssignment.class)
        .withConstraintProviderClass(TimetableConstraintProvider.class)
        .withTerminationConfig(
            new TerminationConfig()
                .withSecondsSpentLimit(300L)
                .withUnimprovedSecondsSpentLimit(60L))
        .withPhases(
            // Phase 1: Build initial solution
            // FIRST_FIT_DECREASING sorts entities by difficulty before assigning
            // (now works because LessonAssignment has difficultyComparatorClass)
            new ConstructionHeuristicPhaseConfig()
                .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT_DECREASING)
                .withTerminationConfig(new TerminationConfig().withSecondsSpentLimit(60L)),

            // Phase 2: Improve solution
            // Uses hill climbing, tabu search, simulated annealing, etc.
            new LocalSearchPhaseConfig()
                .withTerminationConfig(new TerminationConfig().withSecondsSpentLimit(270L)));
  }

  @Bean
  public SolverFactory<TimetableSolution> solverFactory(SolverConfig solverConfig) {
    return SolverFactory.create(solverConfig);
  }

  @Bean
  public SolverManager<TimetableSolution, java.util.UUID> solverManager(
      SolverFactory<TimetableSolution> solverFactory) {

    // Configure parallel solving if needed
    SolverManagerConfig config = new SolverManagerConfig();
    config.setParallelSolverCount("AUTO");

    return SolverManager.create(solverFactory, config);
  }

  /**
   * SolutionManager replaces the deprecated ScoreManager. Used by PermutationService to evaluate
   * constraint scores without re-solving. Must share the same SolverFactory to use the same
   * constraint configuration.
   */
  @Bean
  public SolutionManager<TimetableSolution, HardSoftScore> solutionManager(
      SolverFactory<TimetableSolution> solverFactory) {
    return SolutionManager.create(solverFactory);
  }
}
