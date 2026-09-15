# Revisión de código — Entrega 1

> **Estado: aplicado.** Los 14 hallazgos de este documento están corregidos en el código.
> `mvn clean test` → **86/86 en verde** (eran 60). Los defectos D1, D2 y D3 tienen ahora tests de
> regresión que fallaban antes del arreglo. `DESIGN.md` está actualizado con las decisiones nuevas.
> Este archivo queda como registro de qué se encontró, por qué, y qué se hizo.
>
> Cambios respecto de lo propuesto originalmente, donde la implementación resultó mejor:
>
> - **S2** — el catálogo de penalizaciones se movió a `ChallengeSpec` (no quedó duplicado dentro de
>   `PenaltyScoringRule`), así `validateIncidents` y `score` usan una sola fuente de verdad.
> - **S3** — a `TiebreakRule.description()` no se lo borró: se introdujo
>   `AppliedTiebreak(code, description)`, que le da cliente a los dos métodos y cierra el agujero de
>   explicabilidad de la tabla de posiciones.
> - **S4** — se hizo el arreglo de fondo, no el mínimo: siete casos de uso de consulta y **cero**
>   getters de puertos de salida en el composition root.
> - **Extra** — se eliminaron dos métodos públicos sin ningún llamador que aparecieron al revisar:
>   `ScoreBreakdown.empty()` y `TeamRegistration.documents()`.

Revisión del módulo de dominio contra los criterios de la materia: Clean Architecture (regla de
dependencia e inversión de dependencias), SOLID, calidad de nombres y cobertura de la consigna.

**Estado de partida.** 135 clases de producción, 17 de test, 60 tests. La arquitectura de paquetes
es correcta y el `DESIGN.md` está bien argumentado. Lo que sigue son los puntos donde el código no
cumple lo que el propio `DESIGN.md` afirma, más dos defectos de comportamiento verificados.

**Verificación realizada:** `mvn test -Dmaven.compiler.release=21` → 60/60 en verde. Los defectos D1
y D2 se comprobaron con tests que fallan contra el código actual (transcriptos abajo).

---

## Resumen priorizado

| # | Tema | Severidad | Principio | Estado |
| --- | --- | --- | --- | --- |
| B1 | El proyecto no compila con JDK 21 (LTS) | Bloqueante | — | ✅ |
| D1 | La apelación queda ACEPTADA aunque la corrección se rechace | Alta | SRP / atomicidad | ✅ |
| D2 | Se pueden programar turnos fuera del período de la competencia | Alta | Invariante perdida | ✅ |
| D3 | `TeamRegistration` no protege sus transiciones de estado | Media | Encapsulamiento | ✅ |
| S1 | `TeamScoreSummary` depende de `PenaltyScoringRule` | Alta | **DIP** | ✅ |
| S2 | `ScoringRule.apply` tiene contrato divergente | Alta | **LSP** | ✅ |
| S3 | `ScoringRule.code()`, `EligibilityRule.code()`, `TiebreakRule.description()` sin ningún cliente | Media | **ISP** | ✅ |
| S4 | El composition root expone los puertos de salida | Media | Clean Architecture | ✅ |
| S5 | Códigos de regla como `String` suelto | Media | Primitive Obsession | ✅ |
| S6 | Política de negocio dentro del adaptador en memoria | Media | Clean Architecture | ✅ |
| S7 | `IdGenerator` con prefijos mágicos por caso de uso | Baja | DIP | ✅ |
| S8 | `PenaltyScoringRule` mutable y asimétrico con sus pares | Baja | Consistencia | ✅ |
| N1-N6 | Nombres (ver sección de nombres) | Baja-Media | Clean Code | ✅ |
| T1 | Los tests dependen de `DemoRulebook`, que vive en `src/main` | Media | Dirección de dependencias | ✅ |

---

## B1 — El proyecto no compila con JDK 21

`pom.xml` fija `<maven.compiler.release>25</maven.compiler.release>`. En una máquina con JDK 21
(LTS) el build muere antes de compilar:

```
ERROR Fatal error compiling: error: release version 25 not supported
```

Compilé y corrí la suite completa con `release 21`: **60/60 tests pasan**. Es decir, no se usa
ninguna API ni construcción posterior a 21 — el único uso "moderno" es `List.getLast()`, que es de
21. El target 25 no compra nada y bloquea al corrector.

**Arreglo.** Bajar a 21 y hacer explícito el requisito:

```xml
<properties>
  <maven.compiler.release>21</maven.compiler.release>
  ...
</properties>
```

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <version>3.5.0</version>
  <executions>
    <execution>
      <id>require-jdk</id>
      <goals><goal>enforce</goal></goals>
      <configuration>
        <rules><requireJavaVersion><version>[21,)</version></requireJavaVersion></rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Y corregir el `README.md`, que hoy dice "JDK 25".

---

## D1 — La apelación queda ACEPTADA aunque la corrección se rechace

`ResolveAppealUseCase.execute` muta el agregado **antes** de validar la corrección:

```java
if (command.accepted()) {
    appeal.accept(decision);                                   // (1) muta
    command.correction().ifPresent(c -> correct(appeal, c, actor)); // (2) valida y puede tirar
}
appeals.save(appeal);                                          // (3) nunca se llega si (2) falla
```

Si `correct()` lanza (medición inválida), nunca se ejecuta `appeals.save(appeal)` — pero **el
agregado ya quedó mutado**, y como `InMemoryAppealRepository.findById` devuelve la *misma
instancia* que guardó, la mutación es visible para siempre. Test que lo demuestra:

```java
@Test
void appealIsLeftAcceptedEvenThoughTheCorrectionWasRejected() {
    AppealId appealId = /* ... apelación sobre un run capturado ... */;

    // OBJECTIVES es OBJECTIVE_COUNT: 4.5 tiene que ser rechazado por ChallengeSpec.validate
    MeasurementSet invalid = MeasurementSet.empty()
            .with(DemoRulebook.TIME, MetricValue.of("95.5"))
            .with(DemoRulebook.OBJECTIVES, MetricValue.of("4.5"))
            .with(DemoRulebook.ENERGY, MetricValue.of("42"));

    assertThrows(DomainException.class, () -> module.resolveAppeal()
            .execute(new ResolveAppeal.Command(appealId, true, "judge", "why",
                    Optional.of(new ResolveAppeal.Correction(invalid, List.of())), "judge")));

    assertEquals(AppealStatus.SUBMITTED,
            module.appeals().findById(appealId).orElseThrow().status());
}
// expected: <SUBMITTED> but was: <ACCEPTED>
```

Hay dos problemas encadenados:

1. **Orden.** Se muta antes de validar. Regla general: validar todo, después mutar, después guardar.
2. **El adaptador en memoria oculta la falta de atomicidad.** Al devolver la instancia viva, una
   mutación sin `save()` "persiste". Cuando en la entrega 2 aparezca un adaptador real, los tests
   que hoy pasan van a empezar a describir un comportamiento distinto. El `DESIGN.md` §5.8 reconoce
   que no hay transaccionalidad; el problema no es ése, es que el adaptador de test **enmascara** la
   consecuencia.

**Arreglo — validar primero:**

```java
@Override
public AppealStatus execute(Command command) {
    Appeal appeal = appeals.findById(command.appealId())
            .orElseThrow(() -> NotFoundException.of("Appeal", command.appealId().value()));

    // 1. Resolver todo lo que puede fallar ANTES de tocar el agregado.
    Optional<PendingCorrection> pending = command.accepted()
            ? command.correction().map(correction -> validate(appeal, correction))
            : Optional.empty();

    // 2. Recién ahora se muta.
    AppealDecision decision = new AppealDecision(command.reviewer(), command.rationale(), clock.instant());
    if (command.accepted()) {
        appeal.accept(decision);
    } else {
        appeal.reject(decision);
    }
    appeals.save(appeal);

    pending.ifPresent(correction -> apply(appeal, correction, command.actor()));
    auditLog.record(/* ... */);
    return appeal.status();
}

/** Comprueba que la corrección es admisible para el reglamento fijado en la corrida. */
private PendingCorrection validate(Appeal appeal, Correction correction) {
    RunResult run = runResults.findById(appeal.runId())
            .orElseThrow(() -> NotFoundException.of("RunResult", appeal.runId().value()));
    Round round = rounds.findById(run.roundId())
            .orElseThrow(() -> NotFoundException.of("Round", run.roundId().value()));
    ChallengeSpec challenge = rulebooks.find(round.competitionId(), run.rulebookVersion())
            .orElseThrow(() -> NotFoundException.of("Rulebook", run.rulebookVersion().toString()))
            .challenge(run.challengeId());
    challenge.validate(correction.measurements());
    challenge.validateIncidents(correction.incidents());   // ver S2
    return new PendingCorrection(run, correction);
}

private record PendingCorrection(RunResult run, Correction correction) {
}
```

Y para que el adaptador en memoria deje de mentir, que devuelva una copia defensiva o —más simple y
más honesto— que los tests de caso de uso no puedan leer estado que no fue guardado. La opción
barata: documentar en el puerto que `save` es obligatorio para persistir y agregar un test de
contrato del repositorio.

---

## D2 — Se pueden programar turnos fuera del período de la competencia

`Competition.period()` sólo se usa en `RegisterTeamUseCase`, como fecha de referencia de edad.
`ScheduleRoundUseCase` nunca lo consulta. Verificado:

```java
@Test
void heatsCanBeScheduledOutsideTheCompetitionPeriod() {
    // la competencia corre del 2026-03-01 al 2026-03-05
    RoundId roundId = edition.scheduleRound(1,
            List.of(edition.heat(delta, "A9", LocalDateTime.of(2027, 12, 25, 3, 0))));
    // >>> heat scheduled at 2027-12-25T03:00   ← aceptado sin chistar
}
```

La consigna pide explícitamente "Crear temporada, competencia, categorías y **fechas**" y
"Programación: crear rondas, turnos, pistas y asignaciones de jueces". Hoy el período de la
competencia es decorativo. Además `Season.requireCompetitionPeriodInside` demuestra que el patrón ya
existe en el modelo — sólo falta el escalón siguiente.

**Arreglo — la invariante pertenece a `Competition`:**

```java
// domain/competition/Competition.java
public void requireDateWithinPeriod(LocalDate date) {
    if (!period.contains(date)) {
        throw new DomainException(
                "date " + date + " is outside the period of competition " + name);
    }
}
```

```java
// application/usecase/ScheduleRoundUseCase.java — dentro del for de HeatDraft
for (HeatDraft draft : command.heats()) {
    requireEligibleTeam(command, draft);
    competition.requireDateWithinPeriod(draft.slot().start().toLocalDate());
    competition.requireDateWithinPeriod(draft.slot().end().toLocalDate());
    ...
}
```

Test que lo fija:

```java
@Test
void rejectsAHeatScheduledOutsideTheCompetitionPeriod() {
    TeamId delta = edition.registerEligibleTeam("Delta Bots");

    DomainException error = assertThrows(DomainException.class, () -> edition.scheduleRound(1,
            List.of(edition.heat(delta, "A1", LocalDateTime.of(2027, 12, 25, 3, 0)))));

    assertTrue(error.getMessage().contains("outside the period"));
}
```

---

## D3 — `TeamRegistration` no protege sus transiciones

`Appeal` protege su máquina de estados con cuidado (`resolveWith` rechaza resolver dos veces y
rechaza decisiones anteriores a la presentación). `TeamRegistration` no protege nada:

```java
public void accept() {
    this.status = RegistrationStatus.ACCEPTED;   // desde REJECTED también
    this.rejectionReasons = List.of();
}

public void reject(List<String> reasons) {       // desde ACCEPTED también
    ...
}
```

Cualquier código puede aceptar un equipo ya rechazado, o aceptarlo dos veces, y las razones de
rechazo desaparecen sin rastro. Es una inconsistencia entre dos agregados del mismo modelo: si el
criterio es "las invariantes viven en la entidad" (`DESIGN.md` §4.5), tiene que valer para los dos.

**Arreglo:**

```java
public void accept() {
    requireDecidable();
    this.status = RegistrationStatus.ACCEPTED;
    this.rejectionReasons = List.of();
}

public void reject(List<String> reasons) {
    requireDecidable();
    if (reasons.isEmpty()) {
        throw new DomainException("a rejection requires at least one reason");
    }
    this.status = RegistrationStatus.REJECTED;
    this.rejectionReasons = List.copyOf(reasons);
}

private void requireDecidable() {
    if (status != RegistrationStatus.SUBMITTED) {
        throw new DomainException(
                "registration of team " + name + " was already resolved as " + status);
    }
}
```

---

## S1 — DIP: el ranking depende de una implementación concreta de puntaje

Éste es el hallazgo arquitectónico más importante.

```java
// domain/ranking/TeamScoreSummary.java
import com.dps.roboleague.domain.scoring.rule.PenaltyScoringRule;   // ← ranking → scoring.rule

public Points penaltyPoints() {
    return runs.stream()
            .map(run -> run.breakdown().totalFor(PenaltyScoringRule.CODE))   // ← string mágico
            .reduce(Points.ZERO, Points::plus);
}
```

Un concepto de alto nivel (el resumen de puntaje de un equipo, que alimenta los desempates) importa
una **clase concreta de la capa de implementación de reglas** y filtra por su nombre de código. Es
exactamente lo contrario de "los detalles dependen de las reglas de negocio": acá la política
depende del detalle, y encima por un `String`.

Consecuencias reales, no teóricas:

- Renombrar `PenaltyScoringRule.CODE` de `"PENALTIES"` a cualquier otra cosa rompe silenciosamente
  `FewestPenaltiesTiebreak` — compila, corre, y devuelve 0 para todos los equipos. El desempate
  deja de funcionar sin que ningún test se entere salvo el que asume el string.
- Si mañana el reglamento define dos reglas de penalización distintas (por ejemplo, penalizaciones
  técnicas y de conducta), `penaltyPoints()` sólo ve una.
- Un reglamento que quiera componer penalizaciones dentro de un `CompositeScoringRule` anidado
  funciona por casualidad, porque el código se propaga en cada contribución.

**Arreglo — modelar la *naturaleza* de la contribución en el dominio, no el nombre de la clase.**
Agregar un concepto de dominio al desglose:

```java
// domain/scoring/ContributionKind.java  (nuevo)
package com.dps.roboleague.domain.scoring;

/** Qué representa una contribución dentro del puntaje, con independencia de qué regla la produjo. */
public enum ContributionKind {
    EARNED,    // puntos ganados por desempeño
    BONUS,     // bonificación otorgada por superar un umbral
    PENALTY    // deducción por un incidente
}
```

```java
// domain/scoring/ScoreContribution.java
public record ScoreContribution(String ruleCode, ContributionKind kind, String explanation, Points points) {

    public ScoreContribution {
        Objects.requireNonNull(points, "points are required");
        Objects.requireNonNull(kind, "contribution kind is required");
        if (ruleCode == null || ruleCode.isBlank() || explanation == null || explanation.isBlank()) {
            throw new DomainException("a score contribution requires a rule code and an explanation");
        }
    }
}
```

```java
// domain/scoring/ScoreBreakdown.java — reemplaza totalFor(String)
public Points totalOf(ContributionKind kind) {
    return contributions.stream()
            .filter(contribution -> contribution.kind() == kind)
            .map(ScoreContribution::points)
            .reduce(Points.ZERO, Points::plus);
}
```

```java
// domain/ranking/TeamScoreSummary.java — ya no importa nada de scoring.rule
public Points penaltyPoints() {
    return runs.stream()
            .map(run -> run.breakdown().totalOf(ContributionKind.PENALTY))
            .reduce(Points.ZERO, Points::plus);
}
```

Ahora el ranking depende de un concepto del dominio (`ContributionKind`), no de una clase de
implementación, y el compilador protege la relación. Como beneficio lateral, la publicación puede
mostrar "ganado / bonificaciones / penalizaciones" sin conocer ninguna regla concreta, lo que
refuerza el requisito de cálculo explicable.

---

## S2 — LSP: `ScoringRule.apply` significa cosas distintas según la implementación

La interfaz declara:

```java
List<ScoreContribution> apply(ScoringContext context);
```

pero las siete implementaciones no honran el mismo contrato:

| Regla | Falta el dato de entrada | Cantidad de contribuciones |
| --- | --- | --- |
| `TimeScoringRule` | **lanza** `DomainException` (`measurements().require`) | siempre 1 |
| `ObjectiveScoringRule` | **lanza** | siempre 1 |
| `PrecisionScoringRule` | **lanza** | siempre 1 |
| `ResourceScoringRule` | **lanza** | siempre 1 |
| `ThresholdBonusRule` | **lanza** | siempre 1 |
| `JudgePanelScoringRule` | **devuelve ZERO** con explicación | siempre 1 |
| `PenaltyScoringRule` | **lanza** si el código no está en el catálogo | **0..N** |

Un cliente que tiene una `ScoringRule` no puede saber si un dato ausente significa "cero puntos con
explicación" o "excepción", ni cuántas contribuciones va a recibir. Es el mismo olor que
`billableSeconds()` en el caso `Call`: los overrides cambian el *significado* del método, no su
detalle. Y como dice la presentación de la clase 4, *"toda violación de LSP es una violación
potencial de OCP"*: agregar una regla nueva obliga a averiguar cuál de las dos semánticas seguir.

Además, `PenaltyScoringRule` devuelve **cero contribuciones** cuando no hay incidentes, lo que
contradice lo que el propio `DESIGN.md` §2.3 afirma: *"Las reglas emiten contribución incluso cuando
aportan cero"*.

**Arreglo en dos partes.**

**(a) Fijar el contrato en la interfaz y que todos lo cumplan.** La semántica correcta es: `apply`
nunca lanza; siempre explica. La validación de las entradas ocurre una sola vez, al capturar.

```java
// domain/scoring/ScoringRule.java
public interface ScoringRule {

    /**
     * Devuelve al menos una contribución explicada. Nunca lanza por datos ausentes: si el dato
     * que la regla necesita no está, devuelve una contribución de cero puntos explicando por qué.
     * La validez de las mediciones y de los incidentes se verifica al capturar el resultado.
     */
    List<ScoreContribution> apply(ScoringContext context);

    default ScoreBreakdown breakdownFor(ScoringContext context) {
        return new ScoreBreakdown(apply(context));
    }
}
```

```java
// domain/challenge/MeasurementSet.java — el sustituto de require() para las reglas
public Optional<BigDecimal> amountOf(MetricKey key) {
    return find(key).map(MetricValue::amount);
}
```

```java
// domain/scoring/rule/TimeScoringRule.java — patrón a replicar en las otras cuatro
@Override
public List<ScoreContribution> apply(ScoringContext context) {
    return context.measurements().amountOf(metric)
            .map(this::contributionFor)
            .orElseGet(() -> List.of(new ScoreContribution(CODE, ContributionKind.EARNED,
                    "no measurement recorded for " + metric.value(), Points.ZERO)));
}

private List<ScoreContribution> contributionFor(BigDecimal elapsed) {
    BigDecimal referenceSeconds = BigDecimal.valueOf(reference.toMillis()).movePointLeft(3);
    BigDecimal saved = referenceSeconds.subtract(elapsed);
    Points earned = saved.signum() <= 0
            ? Points.ZERO
            : pointsPerSecondSaved.times(saved).cappedAt(maximumPoints);
    return List.of(new ScoreContribution(CODE, ContributionKind.EARNED,
            "%s s against a reference of %s s".formatted(elapsed.toPlainString(),
                    referenceSeconds.toPlainString()), earned));
}
```

```java
// domain/scoring/rule/PenaltyScoringRule.java — siempre al menos una contribución
@Override
public List<ScoreContribution> apply(ScoringContext context) {
    if (context.incidents().isEmpty()) {
        return List.of(new ScoreContribution(CODE, ContributionKind.PENALTY,
                "no incidents reported", Points.ZERO));
    }
    return context.incidents().stream().map(this::contributionFor).toList();
}
```

**(b) Validar los incidentes al capturar, que es donde falta.** Hoy `CaptureRunResultUseCase` valida
el número de intento y las mediciones, pero **nunca los incidentes** — por eso
`PenaltyScoringRule` se ve obligada a lanzar durante el cálculo, que es tarde y en el lugar
equivocado. Falta un eslabón:

```java
// domain/challenge/ChallengeSpec.java
public record ChallengeSpec(ChallengeId id, String name, List<MetricDefinition> metrics,
        ScoringRule scoringRule, List<PenaltyDefinition> penalties, int maximumAttempts) {

    public void validateIncidents(List<IncidentReport> incidents) {
        Set<PenaltyCode> defined = penalties.stream().map(PenaltyDefinition::code)
                .collect(Collectors.toUnmodifiableSet());
        incidents.stream()
                .map(IncidentReport::code)
                .filter(code -> !defined.contains(code))
                .findFirst()
                .ifPresent(unknown -> {
                    throw new DomainException("penalty " + unknown.value()
                            + " is not defined for challenge " + name);
                });
    }
}
```

```java
// application/usecase/CaptureRunResultUseCase.java
challenge.requireAttemptWithinLimit(command.attemptNumber());
challenge.validate(command.measurements());
challenge.validateIncidents(command.incidents());   // ← nuevo
requireUnusedAttempt(command);
```

El catálogo de penalizaciones pasa a estar donde el `DESIGN.md` dice que está ("el reglamento define
el catálogo"), el error aparece al capturar y no al publicar posiciones, y `ScoringRule` queda con
un contrato único que toda implementación respeta.

---

## S3 — ISP: tres métodos de contrato que nadie llama

Busqué todos los sitios de invocación. Tres métodos existen sólo para ser implementados:

**`ScoringRule.code()`** — 0 llamadas polimórficas. Ocho implementaciones lo definen (siete de ellas
duplicando un `public static final String CODE` que sí se usa, pero como constante, no a través de
la interfaz).

**`EligibilityRule.code()`** — 0 llamadas polimórficas. Cada regla usa su propia constante `CODE`
inline al construir la violación. `EligibilityPolicy.CODE = "ELIGIBILITY_POLICY"` no se referencia
en ningún lado del repositorio.

**`TiebreakRule.description()`** — 0 llamadas. Tres implementaciones escriben una frase en castellano
técnico que nunca se muestra.

Es el patrón de `isLocal()/isNational()/isInternational()` del caso `Call`: métodos implementados
para llenar un contrato que no sirve a ningún cliente. La presentación lo dice directo: *"las
interfaces están más relacionadas con sus usuarios que con sus implementadores"*.

**Arreglo.** Para `ScoringRule.code()` y `EligibilityRule.code()`: borrarlos. Las constantes `CODE`
se quedan, porque sí tienen uso real (etiquetar contribuciones y violaciones).

```java
public interface ScoringRule {
    List<ScoreContribution> apply(ScoringContext context);

    default ScoreBreakdown breakdownFor(ScoringContext context) {
        return new ScoreBreakdown(apply(context));
    }
}

public interface EligibilityRule {
    List<EligibilityViolation> evaluate(EligibilityRequest request);
}
```

Esto arrastra `CompositeScoringRule.code`, que además es un campo **obligatorio** (el constructor
tira si viene en blanco), recibe `"RESCUE_SCORE"` en el demo y **nunca se lee**, porque `apply()`
aplana las contribuciones de los hijos:

```java
public record CompositeScoringRule(List<ScoringRule> rules) implements ScoringRule {

    public CompositeScoringRule {
        rules = List.copyOf(rules);
        if (rules.isEmpty()) {
            throw new DomainException("a composite scoring rule requires at least one rule");
        }
    }

    public static CompositeScoringRule of(ScoringRule... rules) {
        return new CompositeScoringRule(List.of(rules));
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        return rules.stream().flatMap(rule -> rule.apply(context).stream()).toList();
    }
}
```

Para `TiebreakRule.description()` la decisión mejor no es borrarlo sino **darle el cliente que le
falta**, porque hay un agujero de explicabilidad real: `RankingService` registra sólo `rule.code()`,
así que dos `FastestMetricTiebreak` configurados con métricas distintas producen el mismo texto
`"FASTEST_METRIC"` y la tabla no dice cuál criterio resolvió el empate. Con `description()` sí lo
dice:

```java
// domain/ranking/RankingService.java
private List<String> appliedTiebreaks(TeamScoreSummary previous, TeamScoreSummary current,
        List<TiebreakRule> tiebreakRules) {
    if (previous == null || previous.totalPoints().compareTo(current.totalPoints()) != 0) {
        return List.of();
    }
    return tiebreakRules.stream()
            .filter(rule -> rule.compare(previous, current) != 0)
            .findFirst()
            .map(rule -> List.of(rule.description()))   // era rule.code()
            .orElse(List.of());
}
```

(Si prefieren conservar el código para ordenar por máquina, lo correcto es que `StandingEntry` lleve
un `record AppliedTiebreak(String code, String description)` en lugar de `List<String>`.)

---

## S4 — El composition root funciona como service locator

`RoboLeagueModule` expone siete getters de puertos de **salida**:

```java
public CompetitionRepository competitions() { ... }
public TeamRegistrationRepository registrations() { ... }
public RoundRepository rounds() { ... }
public AppealRepository appeals() { ... }
public RunResultRepository runResults() { ... }
public StandingsRepository standings() { ... }
public AuditLog auditLog() { ... }
```

Se usan en 13 lugares entre `DemoScenario` y los tests. El caso más elocuente aparece en las dos
primeras líneas del demo y del fixture de tests:

```java
CategoryId categoryId = module.competitions().findById(competitionId).orElseThrow()
        .categories().getFirst().id();
```

Un adaptador de entrada (el demo) está leyendo un puerto de salida porque **no existe ningún caso de
uso de lectura**. Eso es precisamente el cruce de frontera que Clean Architecture busca impedir, y es
lo que un controller REST de la entrega 2 va a copiar: el `DESIGN.md` §1.3 dice "el dominio define lo
que necesita y la infraestructura obedece", pero acá el driver salta el negocio y va al repositorio.

**Arreglo de fondo:** casos de uso de consulta en `port.in`, y borrar los getters.

```java
// application/port/in/FindCompetition.java
public interface FindCompetition {

    View execute(Command command);

    record Command(CompetitionId competitionId) {
    }

    record View(CompetitionId id, String name, DateRange period, List<CategoryView> categories) {
    }

    record CategoryView(CategoryId id, String name, AgeRange ageRange, RobotClass robotClass) {
    }
}
```

Análogos para `GetStandings`, `FindRunResult` y `FindAuditTrail` (los otros tres getters que usan
los tests).

**Arreglo mínimo**, si el tiempo aprieta: que `CreateCompetition` devuelva las categorías que creó,
lo que elimina de una el peor sitio de llamada:

```java
public interface CreateCompetition {

    Result execute(Command command);

    record Result(CompetitionId competitionId, List<CategoryId> categoryIds) {

        public CategoryId firstCategory() {
            return categoryIds.getFirst();
        }
    }
    ...
}
```

En cualquier caso, `RoboLeagueModule` no debería exponer ni un solo puerto de salida: es una decisión
de una línea que cambia lo que el diseño *permite*, que es lo que la materia evalúa.

---

## S5 — Códigos de regla como `String` suelto

`ScoreContribution.ruleCode` es `String`; `ScoreBreakdown.totalFor(String)` también;
`ScheduleConflict.code` también, con `ScheduleConflictDetector.ARENA_BUSY / TEAM_BUSY / JUDGE_BUSY`
como constantes `String`.

```java
breakdown.totalFor("PENALTIES")   // ok
breakdown.totalFor("PENALTIE")    // compila, corre, devuelve 0.00 en silencio
```

El `DESIGN.md` §4.6 argumenta muy bien contra la obsesión por primitivos ("con `String` o
`BigDecimal` sueltos el compilador no ayuda") y después no aplica el criterio a los códigos de regla
ni a los de conflicto.

**Arreglo.** Para los conflictos de agenda, un enum es directo y no cuesta nada:

```java
// domain/schedule/ScheduleConflictType.java
public enum ScheduleConflictType {
    ARENA_BUSY,
    TEAM_BUSY,
    JUDGE_BUSY
}

// domain/schedule/ScheduleConflict.java
public record ScheduleConflict(ScheduleConflictType type, String detail) {

    public ScheduleConflict {
        Objects.requireNonNull(type, "conflict type is required");
        if (detail == null || detail.isBlank()) {
            throw new DomainException("a schedule conflict requires a detail");
        }
    }
}
```

Para los códigos de puntaje, `ContributionKind` (S1) ya resuelve la consulta que importa. Si además
quieren conservar el código textual por regla, que sea un value object:

```java
// domain/scoring/ScoringRuleCode.java
public record ScoringRuleCode(String value) {

    public ScoringRuleCode {
        if (value == null || value.isBlank()) {
            throw new DomainException("a scoring rule code requires a non blank value");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
    }

    public static ScoringRuleCode of(String value) {
        return new ScoringRuleCode(value);
    }
}
```

---

## S6 — Política de negocio dentro del adaptador en memoria

```java
// infrastructure/memory/InMemoryStandingsRepository.java
@Override
public void save(Standings standings) {
    List<Standings> history = revisions.computeIfAbsent(...);
    history.removeIf(existing -> existing.revision() == standings.revision());   // ← regla de negocio
    history.add(standings);
}
```

"Publicar reemplaza la revisión provisional, hay una sola fila por revisión" es una **decisión del
negocio**, y hoy vive en un adaptador. Un adaptador SQL que haga `INSERT` en lugar de `UPSERT`
cambia el significado de `findHistory` y de `findLatest` sin que nada lo detecte. Mismo patrón en
`InMemoryRulebookRepository.save`.

Que el `DESIGN.md` §3.3 diga "el repositorio conserva todas las revisiones" y el adaptador borre la
versión provisional al publicar es justamente la clase de desalineación que este acoplamiento
produce.

**Arreglo.** Hacer el contrato explícito en el puerto y protegerlo con un test de contrato que todo
adaptador futuro tenga que pasar:

```java
// application/port/out/StandingsRepository.java
public interface StandingsRepository {

    /**
     * Guarda una revisión. Si ya existe una revisión con el mismo número para esa competencia y
     * categoría, la reemplaza: una revisión tiene un único estado vigente (provisional o
     * definitiva). Las revisiones anteriores no se tocan.
     */
    void save(Standings standings);
    ...
}
```

```java
// src/test/java/com/dps/roboleague/application/port/out/StandingsRepositoryContractTest.java
abstract class StandingsRepositoryContractTest {

    protected abstract StandingsRepository repository();

    @Test
    void publishingReplacesTheProvisionalRevisionAndKeepsTheEarlierOnes() { ... }

    @Test
    void findLatestReturnsTheHighestRevision() { ... }
}

class InMemoryStandingsRepositoryTest extends StandingsRepositoryContractTest {
    @Override protected StandingsRepository repository() { return new InMemoryStandingsRepository(); }
}
```

Cuando en la entrega 2 aparezca `JpaStandingsRepository`, hereda la clase y el contrato se verifica
solo. Es la forma barata de demostrar que la inversión de dependencias se sostiene con un adaptador
nuevo, que es exactamente lo que la materia quiere ver.

---

## S7 — `IdGenerator` con prefijos mágicos repartidos por los casos de uso

```java
TeamId.of(idGenerator.nextId("TEAM"))
RunId.of(idGenerator.nextId("RUN"))
RoundId.of(idGenerator.nextId("ROUND"))
HeatId.of(idGenerator.nextId("HEAT"))
AppealId.of(idGenerator.nextId("APPEAL"))
CategoryId.of(idGenerator.nextId("CATEGORY"))
CompetitionId.of(idGenerator.nextId("COMPETITION"))
SeasonId.of(idGenerator.nextId("SEASON"))
```

La relación prefijo↔tipo no está garantizada por nada: `RunId.of(idGenerator.nextId("TEAM"))`
compila y produce un id perfectamente válido y perfectamente equivocado. Además el formato de los
identificadores —que es un detalle— queda esparcido por ocho casos de uso.

**Arreglo:**

```java
// application/port/out/IdGenerator.java
public interface IdGenerator {

    SeasonId nextSeasonId();

    CompetitionId nextCompetitionId();

    CategoryId nextCategoryId();

    TeamId nextTeamId();

    RoundId nextRoundId();

    HeatId nextHeatId();

    RunId nextRunId();

    AppealId nextAppealId();
}
```

```java
// infrastructure/id/SequentialIdGenerator.java
public final class SequentialIdGenerator implements IdGenerator {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override public SeasonId nextSeasonId() { return SeasonId.of(next("SEASON")); }
    @Override public TeamId nextTeamId() { return TeamId.of(next("TEAM")); }
    // ...

    private String next(String prefix) {
        return prefix + "-" + counters.computeIfAbsent(prefix, key -> new AtomicInteger()).incrementAndGet();
    }
}
```

Los casos de uso quedan `RunId runId = idGenerator.nextRunId();` y el formato vive en un solo lugar,
que es la infraestructura.

---

## S8 — `PenaltyScoringRule` es mutable y asimétrico con sus pares

Las otras seis reglas son `record` inmutables. Ésta es una clase con un `LinkedHashMap` que se llena
en el constructor:

```java
public final class PenaltyScoringRule implements ScoringRule {

    private final Map<PenaltyCode, PenaltyDefinition> catalog = new LinkedHashMap<>();

    public PenaltyScoringRule(Collection<PenaltyDefinition> definitions) {
        definitions.forEach(definition -> catalog.put(definition.code(), definition));
    }
```

La referencia es `final` pero el mapa no es inmutable, y la clase rompe el criterio que el
`DESIGN.md` §2.1 declara ("cada regla es además un value object inmutable"). Si además se aplica S2
(b), el catálogo se mueve al `ChallengeSpec` y esta regla se simplifica mucho.

**Arreglo (si el catálogo se queda en la regla):**

```java
public record PenaltyScoringRule(Map<PenaltyCode, PenaltyDefinition> catalog) implements ScoringRule {

    public static final String CODE = "PENALTIES";

    public PenaltyScoringRule {
        catalog = Map.copyOf(catalog);
    }

    public static PenaltyScoringRule of(Collection<PenaltyDefinition> definitions) {
        return new PenaltyScoringRule(definitions.stream()
                .collect(Collectors.toUnmodifiableMap(PenaltyDefinition::code, definition -> definition)));
    }
    ...
}
```

---

## Nombres

La calidad general de los nombres es **buena**: inglés consistente, vocabulario del dominio, sin
abreviaturas, sin notación húngara, sufijos homogéneos (`*UseCase`, `*Rule`, `*Repository`,
`InMemory*`), métodos que se pueden pronunciar y buscar. Los value objects (`Points`, `AgeRange`,
`DateRange`, `MeasurementSet`, `TimeSlot`) leen como el negocio. Lo que sigue son los casos
puntuales que no cierran.

**N1 — `TeamScoreSummary.bestMeasurement(key)` devuelve el *mínimo*.** Éste es el único problema de
nombres que es además un bug latente:

```java
public Optional<MetricValue> bestMeasurement(MetricKey key) {
    return runs.stream()
            .map(run -> run.measurements().find(key))
            .flatMap(Optional::stream)
            .min(Comparator.comparing(MetricValue::amount));   // ← "mejor" == más chico, siempre
}
```

Para `TIME` el mínimo es el mejor; para `OBJECTIVES` el mínimo es el **peor**. Hoy no explota porque
el único llamador es `FastestMetricTiebreak`, pero el nombre invita a usarlo con cualquier métrica y
devolver silenciosamente lo contrario de lo que promete. Renombrar a lo que realmente hace:

```java
/** El valor más bajo registrado por el equipo para esa métrica. */
public Optional<MetricValue> lowestMeasurement(MetricKey key) { ... }
```

**N2 — `RoboLeagueModule`.** "Module" no dice qué hace; el propio javadoc de la clase dice
"Composition root". Que lo diga el nombre: `RoboLeagueCompositionRoot`. Y sus métodos son *fábricas*
con nombre de *comando*, lo que produce lecturas raras en el call site:

```java
module.createSeason().execute(command);   // ¿crea la temporada y después la ejecuta?
```

Mejor `module.createSeasonUseCase().execute(command)`, o bien nombres de acceso a colaborador
(`seasonCreator()`). Es cosmético pero es lo primero que lee el corrector.

**N3 — `CategoryScoreCollector` no colecta, puntúa.** Su trabajo es calcular desgloses de puntaje por
equipo aplicando el reglamento fijado en cada corrida — el javadoc lo explica bien, el nombre no.
`CategoryScoringService` o `TeamScoreCalculator` describen la responsabilidad. Además sus dos
métodos públicos están en niveles de abstracción distintos (`collect` = categoría entera,
`scoreRun` = una corrida); el nombre nuevo tiene que cubrir los dos.

**N4 — `ScheduleConflictDetector.ARENA_BUSY` y hermanos como `String`.** Ver S5: `enum`.

**N5 — `EligibilityPolicy.CODE`.** Constante pública sin ningún uso en el repositorio. Borrar.

**N6 — `ThresholdBonusRule` imprime el nombre del enum en la explicación al usuario:**

```java
String explanation = "%s %s %s: bonus %s".formatted(metric.value(), comparison, ...);
// → "OBJECTIVES AT_LEAST 5: bonus granted"
```

La explicación es material que ve un juez o un equipo. Agregar al enum el texto legible:

```java
public enum Comparison {
    AT_LEAST("at least"),
    AT_MOST("at most");

    private final String label;

    Comparison(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
```

**Nota sobre los 11 identificadores.** `TeamId`, `RunId`, `CategoryId`… repiten el mismo cuerpo de
cuatro líneas, incluyendo el nombre de la clase como `String` mágico en
`Identifier.validate(value, "TeamId")`. Es duplicación, pero es la duplicación **correcta**: son
tipos distintos a propósito, y cualquier jerarquía compartida los volvería intercambiables, que es
justamente lo que el diseño quiere evitar. Lo dejaría como está, y lo diría en el `DESIGN.md` — es
una decisión defendible que hoy no está documentada.

---

## T1 — Los tests dependen de `DemoRulebook`, que vive en `src/main`

`TestEdition` y cinco clases de test importan `com.dps.roboleague.demo.DemoRulebook`, que es código
de producción cuyo propósito es alimentar el `main`. Consecuencias: tocar el demo rompe los tests, y
el reglamento del demo queda congelado por aserciones de test que no hablan de él.

**Arreglo.** Mover el fixture a `src/test/.../support/RescueEditionFixture` (junto a `TeamFixtures`,
que ya hace exactamente esto bien) y que `DemoRulebook` se construya el suyo. La duplicación entre
ambos es sana: son dos clientes con propósitos distintos.

Falta además cobertura para:

- los dos defectos D1 y D2 (tests propuestos arriba);
- `StandingsRepository.findHistory`, que no tiene ningún llamador de producción — o lo usa
  `GetStandings` (S4) o sobra en el puerto;
- las transiciones de `TeamRegistration` (D3).

---

## Plan de trabajo — ejecutado

Se aplicó en este orden:

1. **B1** — bajar a JDK 21 y arreglar el README. Cinco minutos, desbloquea la corrección.
2. **S1** — `ContributionKind`: rompe la dependencia ranking → `scoring.rule`. Es el hallazgo de
   arquitectura más fuerte y el más fácil de defender en la exposición.
3. **S2** — contrato único de `ScoringRule` + validación de incidentes al capturar. Arregla LSP y
   mueve una validación al lugar correcto.
4. **D1, D2, D3** — los tres defectos, con los tests que los fijan.
5. **S3** — borrar los tres métodos muertos y darle cliente a `description()`.
6. **S4** — como mínimo, que `CreateCompetition` devuelva las categorías y sacar `competitions()`
   del composition root.
7. **S5, S7, S8, N1-N6, T1** — limpieza; ninguno es caro.

`DESIGN.md` quedó actualizado: los puntos §2.1 (inmutabilidad de las reglas), §2.3 (contribución
incluso en cero), §3.3 (conservación de revisiones) y §1.3 (el dominio define, la infraestructura
obedece) ahora describen lo que el código efectivamente hace. Se agregaron además cuatro decisiones
nuevas: §2.1.1 (el contrato único de `ScoringRule`, LSP), §2.3.1 (`ContributionKind`, DIP), §4.6 (por
qué los 11 identificadores duplicados se dejan como están) y §5.9 (por qué no se declaran métodos en
las interfaces "por si acaso").
