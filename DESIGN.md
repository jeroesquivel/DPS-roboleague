# Decisiones de diseño

Este documento enumera las decisiones de diseño del módulo de dominio de RoboLeague: qué patrón o
principio se aplicó, dónde y por qué, qué alternativas se descartaron y qué patrones se decidió no
aplicar junto con sus consecuencias.

## 1. Arquitectura general

### 1.1 Clean Architecture en tres capas con dependencias hacia adentro

**Patrón / principio:** Clean Architecture, Regla de Dependencia, Inversión de Dependencias (DIP).

**Dónde:** organización de paquetes bajo `com.dps.roboleague`.

| Capa | Paquete | Contenido | Depende de |
| --- | --- | --- | --- |
| Dominio | `domain.*` | Entidades, value objects, reglas y servicios de dominio | Nada fuera del dominio y del JDK |
| Aplicación | `application.*` | Casos de uso y contratos (`port.in`, `port.out`) | Dominio |
| Detalles | `infrastructure.*`, `demo`, `Main` | Adaptadores en memoria, composition root y ejecución de ejemplo | Aplicación y dominio |

El dominio no importa ninguna clase de `application` ni de `infrastructure`: la dirección de las
dependencias es siempre hacia el centro. Las reglas de negocio (puntaje, elegibilidad, desempates,
conflictos de agenda) quedan expresadas en clases que no conocen persistencia, framework ni entrada
de datos.

**Por qué:** la consigna explicita que lo determinante es el modelado y la extensibilidad. Aislar el
negocio permite que la entrega 2 agregue API REST y persistencia real escribiendo adaptadores
nuevos, sin tocar el dominio.

**Alternativas descartadas:** arquitectura en capas tradicional (`controller → service → dao`), donde
el dominio termina dependiendo del mecanismo de persistencia; y un único paquete plano, que no
comunica la separación entre negocio y detalles.

### 1.2 Casos de uso como puertos de entrada con comandos propios

**Patrón / principio:** Ports & Adapters, Interface Segregation (ISP), Single Responsibility (SRP).

**Dónde:** `application/port/in/*` (interfaces) y `application/usecase/*UseCase` (implementaciones).

Cada caso de uso es una interfaz con un único método `execute` y un `record Command` anidado que
transporta su entrada; por ejemplo `CaptureRunResult.Command`. La implementación vive aparte y es la
única que conoce los repositorios.

**Por qué:** un consumidor (mañana un controller REST) depende exclusivamente de la operación que
necesita y no de un "servicio de competencias" con veinte métodos. El comando anidado mantiene junta
la operación con su contrato de entrada y evita una explosión de DTOs sueltos.

**Alternativas descartadas:** un `CompetitionService` con todos los métodos (viola ISP y SRP, y crece
sin control); pasar parámetros sueltos en vez de un comando (cada campo nuevo rompe la firma y todos
los llamadores).

### 1.3 Repositorios declarados por la aplicación e implementados afuera

**Patrón / principio:** Repository, DIP.

**Dónde:** interfaces en `application/port/out/*`, implementaciones en `infrastructure/memory/*`.

Los puertos de salida están expresados en el lenguaje del negocio (`findLatest(competitionId,
categoryId)`) y devuelven agregados y `Optional`, nunca filas ni estructuras de base de datos.

**Por qué:** el dominio define lo que necesita y la infraestructura obedece. Los casos de uso se
prueban contra adaptadores en memoria reales, sin base de datos.

**Alternativas descartadas:** un `Repository<T, ID>` genérico con CRUD uniforme, que arrastra
operaciones que ningún caso de uso usa y filtra decisiones de persistencia al dominio; y usar las
implementaciones concretas directamente en los casos de uso, que ataría el negocio al detalle.

### 1.4 Composition root explícito, sin framework de inyección

**Patrón / principio:** Composition Root, Inyección de dependencias por constructor.

**Dónde:** `infrastructure/config/RoboLeagueModule`.

Es el único lugar donde se eligen implementaciones concretas. Todas las demás clases reciben sus
colaboradores por constructor y son inmutables.

**Por qué:** deja ver de un vistazo el grafo completo del sistema y permite que los tests construyan
el módulo con un `Clock` fijo. Además prueba que el dominio funciona sin contenedor.

**Alternativas descartadas:** Spring u otro contenedor (dependencia pesada e innecesaria para una
entrega de dominio); `static` o singletons (imposibilita aislar el estado entre tests).

## 2. Modelado del puntaje

### 2.1 Reglas de puntaje como estrategias intercambiables

**Patrón / principio:** Strategy, Open/Closed (OCP).

**Dónde:** `domain/scoring/ScoringRule` y las implementaciones de `domain/scoring/rule`:
`TimeScoringRule`, `ObjectiveScoringRule`, `PrecisionScoringRule`, `ResourceScoringRule`,
`JudgePanelScoringRule`, `PenaltyScoringRule` y `ThresholdBonusRule`.

El enunciado describe desafíos puntuados por tiempo, objetivos, precisión, consumo de recursos,
evaluación de jueces o combinaciones. Cada criterio es una clase que implementa la misma interfaz y
recibe su configuración por constructor (referencia de tiempo, puntos por objetivo, tolerancia de
consumo).

**Por qué:** agregar un criterio nuevo es agregar una clase; ninguna clase existente se modifica.
Cada regla es además un value object inmutable, testeable de forma aislada.

**Alternativas descartadas:** un método de cálculo con `switch` sobre un `enum` de tipos de desafío
(cada desafío nuevo obliga a editar el mismo método, rompiendo OCP); herencia con una clase base
abstracta de puntaje (acopla las reglas entre sí y no permite combinarlas libremente).

### 2.2 Combinación de criterios mediante Composite

**Patrón / principio:** Composite.

**Dónde:** `domain/scoring/rule/CompositeScoringRule`.

`CompositeScoringRule` implementa `ScoringRule` y contiene una lista de reglas, de modo que un
desafío que combina factores se configura igual que uno simple: `ChallengeSpec` sólo conoce una
`ScoringRule`.

**Por qué:** resuelve el requisito de "una combinación de factores" sin ningún caso especial y admite
anidamiento (bloques de puntaje dentro de bloques).

**Alternativas descartadas:** que `ChallengeSpec` tuviera una lista de reglas y las recorriera él
mismo, lo que duplicaría la lógica de agregación en cada consumidor.

### 2.3 Puntaje explicable: el resultado es un desglose, no un número

**Patrón / principio:** Value Object, Tell Don't Ask.

**Dónde:** `domain/scoring/ScoreContribution`, `ScoreBreakdown` y el retorno de
`CalculateRunScore.RunScore`.

Cada regla devuelve `ScoreContribution` con su código, su explicación textual y los puntos aportados;
el total es la suma de las contribuciones. Las reglas emiten contribución incluso cuando aportan
cero ("bonificación no otorgada", "consumo dentro del margen"), porque la ausencia de puntos también
es información que el juez necesita ver.

**Por qué:** el requisito de "cálculo explicable" exige poder mostrar cómo se llegó a cada puntaje.
Si el dominio devolviera un `BigDecimal`, la explicación habría que reconstruirla afuera, duplicando
las reglas.

**Alternativas descartadas:** loguear el cálculo (la explicación queda fuera del modelo y no es
consultable); recalcular la explicación en la capa de presentación (duplica las reglas y se
desincroniza).

### 2.4 Penalizaciones y bonificaciones como parte del mismo mecanismo

**Patrón / principio:** uniformidad de modelo.

**Dónde:** `PenaltyDefinition`, `IncidentReport`, `PenaltyScoringRule`, `ThresholdBonusRule`.

Una penalización es una contribución negativa y una bonificación una contribución positiva: ambas
aparecen en el mismo desglose. El reglamento define el catálogo de penalizaciones y un incidente
reportado que no figure en él es rechazado.

**Por qué:** el desglose queda completo y auditable en una sola estructura, y el total ya contempla
ajustes sin pasos posteriores.

**Alternativas descartadas:** aplicar las penalizaciones después del cálculo, como un descuento sobre
el total, lo que las dejaría fuera de la explicación y obligaría a un orden implícito de aplicación.

## 3. Reglamento, versionado y recálculo

### 3.1 Reglamento inmutable y versionado

**Patrón / principio:** Value Object inmutable, Builder.

**Dónde:** `domain/rulebook/Rulebook`, `RulebookVersion` y `Rulebook.Builder`.

Un `Rulebook` reúne los desafíos, la política de elegibilidad y los criterios de desempate de una
versión. Publicar un reglamento nunca modifica el anterior: `PublishRulebookUseCase` crea la versión
siguiente y `Competition.activateRulebook` sólo acepta versiones que superen a la vigente. Como el
reglamento tiene seis componentes y se arma por partes, se expone un `Builder`.

**Por qué:** el enunciado exige poder recalcular resultados con exactamente la versión de reglas
correspondiente. Eso sólo es confiable si las versiones publicadas son inmutables.

**Alternativas descartadas:** un reglamento mutable con historial de cambios (cualquier corrección
alteraría resultados ya publicados); guardar sólo la versión vigente (haría imposible el recálculo
histórico).

### 3.2 La versión de reglas se fija en la ronda y viaja con el resultado

**Patrón / principio:** Snapshot de configuración.

**Dónde:** `Round.rulebookVersion`, `RunResult.rulebookVersion`, `CategoryScoreCollector`.

Al programar una ronda se fija la versión vigente; al capturar un resultado esa versión se copia en
el `RunResult`. Puntuar una corrida siempre carga el reglamento por esa versión, no por la vigente.

**Por qué:** un reglamento publicado a mitad del evento no puede cambiar retroactivamente lo ya
corrido. El test `CalculateRunScoreUseCaseTest` verifica justamente que una corrida vieja sigue
puntuando con su reglamento aunque exista una versión nueva activa.

**Alternativas descartadas:** resolver siempre el reglamento activo de la competencia (rompe el
recálculo determinista); guardar el puntaje calculado dentro del resultado (lo vuelve un dato
desincronizable y no explica de dónde salió).

### 3.3 Separación entre generar, publicar y recalcular

**Patrón / principio:** SRP, máquina de estados explícita.

**Dónde:** `GenerateStandingsUseCase`, `PublishStandingsUseCase`, `RecalculateStandingsUseCase` y
`domain/ranking/Standings`.

`Standings` es inmutable y lleva número de revisión y estado (`PROVISIONAL` / `FINAL`). `publish()`
falla si ya es definitiva; `supersede()` abre una revisión nueva provisional conservando la versión
de reglamento. Generar dos veces la misma tabla se rechaza con un mensaje que indica usar el
recálculo, y el repositorio conserva todas las revisiones.

**Por qué:** cubre "diferenciar resultados provisionales y definitivos" y "reprocesar posiciones
después de una corrección" sin que una operación pise silenciosamente a la otra, dejando trazable
cada publicación.

**Alternativas descartadas:** una tabla mutable que se sobrescribe (pierde el histórico y no permite
comparar antes y después de una apelación); publicar automáticamente tras generar (impide revisar el
resultado provisional).

### 3.4 Desempates como cadena de comparadores configurable

**Patrón / principio:** Strategy + composición de `Comparator`.

**Dónde:** `domain/ranking/TiebreakRule` (extiende `Comparator<TeamScoreSummary>`), sus
implementaciones en `domain/ranking/rule` y `RankingService`.

`RankingService` arma el comparador final: puntaje total descendente y luego, en orden, cada regla de
desempate del reglamento. Si ninguna regla separa a dos equipos, comparten posición y la siguiente
posición salta, y cada entrada registra qué regla resolvió el desempate.

**Por qué:** el orden de los criterios de desempate es una decisión del reglamento, no del código; y
registrar la regla aplicada mantiene la coherencia con el requisito de explicabilidad.

**Alternativas descartadas:** un comparador único con toda la lógica (no configurable por edición);
Chain of Responsibility con objetos propios (equivalente en comportamiento pero reimplementando lo
que `Comparator.thenComparing` ya ofrece).

## 4. Elegibilidad, agenda y resultados

### 4.1 Elegibilidad con Specification y acumulación de violaciones

**Patrón / principio:** Specification, Composite, OCP.

**Dónde:** `domain/eligibility/EligibilityRule`, `EligibilityPolicy` y las reglas `AgeRangeRule`,
`TeamCompositionRule`, `RobotClassRule`, `RobotSpecificationRule`, `RequiredDocumentsRule`.

Cada restricción es una regla que devuelve la lista de violaciones que encuentra. `EligibilityPolicy`
es a la vez una regla y la composición de todas: devuelve un `EligibilityVerdict` con todas las
violaciones, no con la primera.

**Por qué:** un equipo debe recibir de una sola vez todo lo que tiene que corregir. Además, las
restricciones varían por edición y categoría, así que se configuran en el reglamento en lugar de
estar cableadas.

**Alternativas descartadas:** validaciones con corte temprano (`if` encadenados que abortan en el
primer error), que obligan a registrarse varias veces para descubrir todos los problemas; validación
por anotaciones sobre el registro, que no permite reglas dependientes de la categoría ni de la fecha
de referencia.

### 4.2 Detección de conflictos de agenda como servicio de dominio

**Patrón / principio:** Domain Service.

**Dónde:** `domain/schedule/ScheduleConflictDetector`, usado por `ScheduleRoundUseCase`.

Detectar que una pista, un equipo o un juez ya están ocupados requiere mirar turnos de varias rondas,
así que la regla no pertenece a ninguna entidad. El caso de uso reúne los turnos existentes y el
servicio decide; `Round` conserva la invariante que sí le corresponde (un equipo no puede tener dos
turnos en la misma ronda).

**Por qué:** mantiene la regla en el dominio y testeable sin repositorios, sin forzarla dentro de una
entidad que no tiene toda la información.

**Alternativas descartadas:** poner la validación en el caso de uso (mezcla orquestación con negocio y
no se puede reutilizar); poner la validación en `Round` (no ve los turnos de las demás rondas).

### 4.3 Auditoría: el resultado conserva el original y todas las modificaciones

**Patrón / principio:** historial de correcciones dentro del agregado.

**Dónde:** `domain/result/RunResult` y `ResultCorrection`.

`RunResult` guarda las mediciones e incidentes originales y una lista de correcciones; las mediciones
vigentes son las de la última corrección. Cada corrección registra momento, responsable, motivo y la
apelación que la originó, y no puede ser anterior a la captura.

**Por qué:** cumple "conservar los valores originales y todas las modificaciones" dentro del modelo,
no en una bitácora externa que podría desincronizarse. El recálculo usa siempre los valores vigentes
y la investigación puede reconstruir el camino completo.

**Alternativas descartadas:** actualizar las mediciones en el lugar y anotar el cambio en un log
(pierde la trazabilidad dentro del agregado); event sourcing completo del resultado (ver 5.1).

### 4.4 Bitácora de auditoría como puerto

**Patrón / principio:** DIP, Observer simplificado.

**Dónde:** `application/port/out/AuditLog`, `domain/audit/AuditEvent` y `AuditAction`.

Cada caso de uso que modifica estado registra un `AuditEvent` tipado con acción, sujeto, responsable
y detalles. Los tests verifican, por ejemplo, que aceptar una apelación deja `RESULT_CORRECTED`.

**Por qué:** la auditoría atraviesa todos los casos de uso y debe poder apuntar mañana a un archivo o
a una base sin tocar el negocio.

**Alternativas descartadas:** eventos de dominio publicados por las entidades con un bus (potente pero
prematuro sin infraestructura asincrónica); registrar la auditoría en el adaptador de persistencia
(perdería el motivo y el responsable de la acción).

### 4.5 Apelaciones como agregado con transiciones protegidas

**Patrón / principio:** máquina de estados en la entidad, encapsulamiento de invariantes.

**Dónde:** `domain/appeal/Appeal`, `ResolveAppealUseCase`.

Una apelación sólo puede resolverse una vez y la decisión no puede ser anterior a su presentación. Si
se acepta con una corrección, el caso de uso valida las mediciones corregidas contra el desafío del
reglamento fijado en la corrida antes de aplicarlas.

**Por qué:** las reglas de transición viven en la entidad, no en el caso de uso, que sólo orquesta. Y
una corrección no puede introducir datos que el propio desafío rechazaría.

**Alternativas descartadas:** un campo de estado editable desde afuera (cualquier código podría dejar
la apelación en un estado inconsistente).

### 4.6 Value objects tipados en lugar de primitivos

**Patrón / principio:** Value Object, evitar Primitive Obsession.

**Dónde:** `domain/shared` (`Points`, `DateRange`, `AgeRange`, e identificadores como `TeamId`,
`RunId`), `domain/challenge` (`MetricKey`, `MetricValue`, `MeasurementSet`).

Los identificadores son records distintos que implementan `Identifier`, de modo que pasar un
`CategoryId` donde se espera un `TeamId` no compila. `Points` normaliza la escala decimal y `MetricValue`
rechaza valores negativos; `MetricKind` valida que un conteo de objetivos sea entero y que una razón
de precisión no supere 1.

**Por qué:** las invariantes se verifican una sola vez, en la construcción, y no se repiten en cada
caso de uso. Con `String` o `BigDecimal` sueltos, el compilador no ayuda y las validaciones se
dispersan.

**Alternativas descartadas:** `UUID`/`String` para todos los ids (intercambiables por error); `double`
para puntajes (errores de redondeo inaceptables en un resultado deportivo).

### 4.7 Tiempo e identificadores inyectados

**Patrón / principio:** DIP aplicado a dependencias ambientales.

**Dónde:** `java.time.Clock` e `IdGenerator` inyectados en los casos de uso;
`infrastructure/id/SequentialIdGenerator`.

**Por qué:** ningún caso de uso llama a `Instant.now()` ni genera ids por su cuenta, así que los tests
corren con un reloj fijo y con identificadores predecibles, y las aserciones sobre marcas de tiempo
no dependen del momento de ejecución.

**Alternativas descartadas:** `Instant.now()` y `UUID.randomUUID()` directos, que vuelven los tests no
determinísticos.

### 4.8 Reutilización del cálculo entre generar y recalcular

**Patrón / principio:** DRY, servicio de aplicación.

**Dónde:** `application/service/CategoryScoreCollector`.

Recorre las rondas de una categoría, puntúa cada corrida con su reglamento fijado y arma los
`TeamScoreSummary` que consume `RankingService`. Lo usan `GenerateStandings`, `RecalculateStandings` y
`CalculateRunScore`.

**Por qué:** si cada caso de uso armara la tabla por su cuenta, generar y recalcular podrían divergir,
que es exactamente el error que el requisito de recálculo busca evitar.

**Alternativas descartadas:** duplicar el recorrido en cada caso de uso; ubicarlo en el dominio, que lo
obligaría a conocer repositorios.

## 5. Patrones que decidimos no aplicar

### 5.1 Event Sourcing

No se aplicó: el estado se guarda como agregados, no como secuencia de eventos. La trazabilidad que
exige la consigna se cubre con el historial de correcciones de `RunResult`, las revisiones de
`Standings` y la bitácora `AuditLog`.

**Consecuencia:** no se puede reconstruir el estado a cualquier instante arbitrario ni reproyectar el
pasado con reglas nuevas; sí se conserva todo lo que el negocio pide auditar. Si más adelante se
exigiera reconstrucción total, el historial actual es el punto de partida para migrar.

### 5.2 CQRS

No se aplicó: los casos de uso de lectura y escritura comparten los mismos puertos y modelos.

**Consecuencia:** consultas de gran volumen (posiciones históricas de todas las categorías) pasarán por
los mismos repositorios y podrán ser menos eficientes. A esta escala, la complejidad de mantener dos
modelos no se justifica.

### 5.3 Eventos de dominio con bus de publicación

No se aplicó: aceptar una apelación no emite un evento que dispare el recálculo; el recálculo es un
caso de uso explícito que el operador invoca.

**Consecuencia:** la cadena corrección → recálculo → publicación queda en manos de quien orquesta, y no
hay reacción automática. A cambio, el flujo es explícito, sincrónico y fácil de auditar, sin
infraestructura de mensajería que esta entrega no tiene.

### 5.4 Framework de inyección de dependencias

No se aplicó: el cableado es manual en `RoboLeagueModule`.

**Consecuencia:** agregar un caso de uso obliga a tocar el composition root. A cambio, el módulo compila
y corre sin dependencias externas y el grafo de dependencias es visible en un archivo.

### 5.5 Framework de mocking

No se aplicó: los tests de integración usan los adaptadores en memoria reales y los unitarios
construyen objetos de dominio directamente.

**Consecuencia:** un cambio en la firma de un puerto obliga a actualizar el adaptador en memoria. A
cambio, los tests verifican comportamiento real y no interacciones simuladas, y no quedan atados a la
implementación interna de los casos de uso.

### 5.6 Repositorio genérico y clase base de entidad

No se aplicó: no hay `Repository<T, ID>` ni `AbstractEntity`.

**Consecuencia:** cada puerto declara sus propias consultas, con algo de repetición entre adaptadores
en memoria. A cambio, ninguna entidad hereda comportamiento que no necesita y cada repositorio expone
sólo lo que el negocio usa (ISP).

### 5.7 Motor de reglas configurable por datos

No se aplicó: las reglas de puntaje se componen en código Java, no se interpretan desde una
configuración externa o un DSL.

**Consecuencia:** publicar un reglamento con una fórmula inédita requiere una clase nueva y un
despliegue, no un archivo de configuración. A cambio, las reglas son tipadas, testeables y depurables;
un intérprete propio hubiera sido la parte más riesgosa del sistema. La interfaz `ScoringRule` deja la
puerta abierta a agregar un adaptador que construya reglas desde datos.

### 5.8 Persistencia real, API REST y seguridad

Fuera del alcance de esta entrega según la consigna. Los adaptadores en memoria existen para poder
ejecutar y probar el dominio de punta a punta.

**Consecuencia:** no hay transaccionalidad ni concurrencia: un caso de uso que escribe en varios
repositorios no es atómico. Los puertos ya están definidos, así que la entrega siguiente sólo agrega
implementaciones.

## 6. Cobertura de los requisitos obligatorios

| Capacidad | Dónde se resuelve |
| --- | --- |
| Configuración del evento | `CreateSeasonUseCase`, `CreateCompetitionUseCase`, `Season`, `Competition`, `Category` |
| Registro de equipos | `RegisterTeamUseCase`, `TeamRegistration`, `Member`, `Robot`, `TeamDocument` |
| Elegibilidad | `EligibilityPolicy` y las reglas de `domain/eligibility/rule` |
| Configuración de desafíos | `ChallengeSpec`, `MetricDefinition`, `domain/scoring/rule/*`, `PenaltyDefinition` |
| Programación | `ScheduleRoundUseCase`, `Round`, `Heat`, `TimeSlot`, `ScheduleConflictDetector` |
| Captura de resultados | `CaptureRunResultUseCase`, `RunResult`, `MeasurementSet`, `JudgeEvaluation`, `IncidentReport` |
| Cálculo explicable | `CalculateRunScoreUseCase`, `ScoreBreakdown`, `ScoreContribution` |
| Ranking | `RankingService`, `TiebreakRule` y sus implementaciones |
| Publicación | `Standings`, `PublicationStatus`, `GenerateStandingsUseCase`, `PublishStandingsUseCase` |
| Apelaciones | `Appeal`, `SubmitAppealUseCase`, `ResolveAppealUseCase` |
| Recálculo | `RecalculateStandingsUseCase`, `CategoryScoreCollector` |
| Auditoría | `RunResult.corrections()`, `Standings.revision()`, `AuditLog`, `AuditEvent` |

## 7. Estrategia de pruebas

Los tests unitarios cubren las reglas donde vive el negocio: cálculo de cada criterio de puntaje y su
composición, validación de mediciones contra el desafío, elegibilidad, desempates y posiciones
compartidas, conflictos de agenda, historial de correcciones y transiciones de apelaciones y
publicación.

Los tests de integración ejercitan los casos de uso contra los adaptadores en memoria a través del
composition root, con un reloj fijo e identificadores secuenciales. Cubren los escenarios de negocio
más relevantes: aceptar y rechazar una inscripción, programar una ronda con conflictos, capturar
resultados con validaciones, obtener el desglose explicable, sostener la versión de reglamento fijada
al capturar, publicar posiciones y el circuito completo de apelación aceptada, corrección y recálculo
que reordena la tabla.
