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
| Aplicación | `application.*` | Casos de uso y contratos | Dominio |
| Detalles | `infrastructure.*`, `demo`, `Main` | Adaptadores en memoria, composition root y ejecución de ejemplo | Aplicación y dominio |

El dominio no importa ninguna clase de `application` ni de `infrastructure`: la dirección de las
dependencias es siempre hacia el centro. Las reglas de negocio (puntaje, elegibilidad, desempates,
conflictos de agenda) quedan expresadas en clases que no conocen persistencia, framework ni entrada
de datos.

**Por qué:** la consigna explicita que lo determinante es el modelado y la extensibilidad. Aislar el
negocio permite que la entrega 2 agregue API REST y persistencia real escribiendo adaptadores
nuevos, sin tocar el dominio.

**Alternativas descartadas:** una implementación por capas (`controller → service → dao`) que
acople la lógica a repositorios concretos; y un único paquete plano, que no comunica la separación
entre negocio y detalles. Una arquitectura por capas también puede invertir sus dependencias:
lo determinante es el acoplamiento efectivo, no el nombre de las capas.

### 1.2 Casos de uso como puertos de entrada con entradas propias

**Patrón / principio:** Ports & Adapters, Interface Segregation (ISP), Single Responsibility (SRP).

**Dónde:** `application/port/in/*` (interfaces) y `application/usecase/*UseCase` (implementaciones).

Cada caso de uso es una interfaz con un único método `execute`. Las operaciones con varias entradas
las agrupan en un `record Command` anidado; por ejemplo `CaptureRunResult.Command`. Las búsquedas
`FindCompetition`, `FindRunResult`, `FindRound`, `FindTeamRegistration` y `FindAppeal` reciben
directamente su identificador tipado: `FindRunResult.execute(RunId)`. La implementación vive aparte
y consulta los repositorios a través de puertos de salida.

**Por qué:** un consumidor (mañana un controller REST) depende exclusivamente de la operación que
necesita. El comando anidado mantiene junta la operación con su contrato de entrada; recibir
directamente un identificador evita un objeto intermedio que no agrega información ni validaciones.
Ambas formas conservan el puerto de entrada y la separación de responsabilidades.

Leer también es un caso de uso: `FindCompetition`, `FindRunResult`, `FindRound`,
`FindTeamRegistration`, `FindAppeal`, `GetStandings` y `FindAuditTrail` son puertos de entrada como
cualquier otro. Sin ellos, quien maneja la aplicación termina yendo al repositorio por su cuenta,
que es exactamente el cruce de frontera que evita esta arquitectura.

**Por qué los de consulta también:** el ejecutable de ejemplo y los tests son adaptadores de
entrada. Si pueden alcanzar un `CompetitionRepository`, el controller REST de la entrega 2 va a
copiar ese atajo y la capa de aplicación deja de ser el único camino al negocio.

Los puertos de consulta contienen las operaciones y sus datos de salida. La transformación de
eventos de auditoría a acciones, usada sólo por los tests, vive en `support/TestEdition.actionsOf`.
`FindCompetition.View` expone sus categorías sin agregar un método para obtener la primera que
ningún consumidor utilizaba.

**Alternativas descartadas:** concentrar responsabilidades sin relación en un servicio y obligar a
todos los consumidores a depender de sus operaciones. Tener varios métodos relacionados no viola
SRP ni ISP por sí solo; aquí se eligieron contratos pequeños por operación. También se descartaron
los comandos que sólo envolvían los identificadores de búsqueda, pasar muchos parámetros sueltos en
operaciones complejas y exponer los repositorios desde el composition root. Un comando agrupa la
entrada, pero agregarle componentes también cambia su constructor y puede exigir adaptar clientes.

### 1.3 Repositorios declarados por la aplicación e implementados afuera

**Patrón / principio:** Repository, DIP.

**Dónde:** interfaces en `application/port/out/*`, implementaciones en `infrastructure/memory/*`.

Los puertos de salida están expresados en el lenguaje del negocio (`findLatest(competitionId,
categoryId)`) y devuelven agregados y `Optional`, nunca filas ni estructuras de base de datos.

Cuando el contrato de un puerto incluye una regla de negocio —"publicar reemplaza la revisión
provisional, las anteriores no se tocan"— se verifica con un test de contrato abstracto,
`StandingsRepositoryContractTest`, que todo adaptador de posiciones hereda. Así la
política no queda escondida en el `save` de un adaptador concreto, donde una implementación con SQL
podría reinterpretarla en silencio.

**Por qué:** el dominio define lo que necesita y la infraestructura obedece. Los casos de uso se
prueban contra adaptadores en memoria reales, sin base de datos.

**Alternativas descartadas:** un `Repository<T, ID>` genérico con CRUD uniforme, que arrastra
operaciones que ningún caso de uso usa y filtra decisiones de persistencia al dominio; y usar las
implementaciones concretas directamente en los casos de uso, que ataría el negocio al detalle.

### 1.4 Composition root explícito, sin framework de inyección

**Patrón / principio:** Composition Root, Inyección de dependencias por constructor.

**Dónde:** `infrastructure/config/RoboLeagueCompositionRoot`.

Es el lugar de ensamblado donde se eligen los adaptadores concretos. Los casos de uso reciben sus
colaboradores por constructor; las entidades pueden tener mutaciones protegidas por métodos de
negocio. **Sólo expone puertos de entrada:** no hay getters de repositorios. Algunas consultas sí
devuelven entidades mutables, por lo que un futuro adaptador de API deberá proyectar su salida a
DTOs y mantener las modificaciones dentro de los casos de uso.

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

**Por qué:** un criterio nuevo implementa `ScoringRule` y se incorpora a la configuración y las
pruebas; el algoritmo que consume reglas no necesita conocer esa clase nueva. Cada regla actual
mantiene su configuración inmutable y se puede probar de forma aislada.

Las siete son `record` inmutables, sin excepción: `PenaltyScoringRule` guarda su catálogo como un
`Map` inmutable construido con `Collectors.toUnmodifiableMap`.

**Alternativas descartadas:** un método de cálculo con `switch` sobre un `enum` de tipos de desafío
(cada desafío nuevo obliga a editar el mismo método, rompiendo OCP); herencia con una clase base
abstracta de puntaje (acopla las reglas entre sí y no permite combinarlas libremente).

### 2.1.1 El contrato de `ScoringRule` es uno solo para todas las implementaciones

**Patrón / principio:** Liskov Substitution (LSP), diseño por contrato.

**Dónde:** `domain/scoring/ScoringRule` y las siete implementaciones de `domain/scoring/rule`.

La interfaz declara explícitamente su contrato: **`apply` devuelve siempre al menos una contribución
explicada y nunca lanza por un dato ausente**. Si a una regla le falta la medición que necesita,
devuelve una contribución de cero puntos que explica la ausencia, en lugar de tirar una excepción.

**Por qué:** un cliente que tiene una `ScoringRule` no puede saber de qué implementación se trata. Si
una regla lanza ante un dato ausente y otra devuelve cero con explicación, el subtipo cambió el
*significado* del método y dejó de ser sustituible por la abstracción. Dos tests parametrizados en
`ScoringRulesTest` recorren las siete reglas simples de su proveedor `everyRule()` y verifican el
contrato. Para comprobar una regla nueva hay que agregarla explícitamente a ese proveedor.

**Consecuencia sobre dónde se valida:** que `apply` no lance obliga a validar las entradas antes, al
capturar el resultado. Por eso el catálogo de penalizaciones vive en `ChallengeSpec` y no dentro de
`PenaltyScoringRule` (ver 2.4): un incidente que el reglamento no define se rechaza al capturar, que
es cuando hay alguien mirando, y no al publicar posiciones.

**Alternativas descartadas:** dejar que cada regla decida si lanza o explica (es el estado del que
partimos: cinco reglas lanzaban, una explicaba y otra hacía las dos cosas); declarar una excepción
chequeada en la firma, que traslada el problema al llamador sin unificar el significado.

### 2.2 Un desafío conoce una lista de reglas, no una regla que a su vez es una lista

**Patrón / principio:** simplicidad deliberada; se prefirió sobre Composite (ver 5.10).

**Dónde:** `ChallengeSpec.scoringRules` y `ChallengeSpec.score`.

`ChallengeSpec` guarda directamente una `List<ScoringRule>`. Al puntuar, `score` aplica cada
regla de la lista, le suma las contribuciones de `PenaltyScoringRule.of(penalties)` (armada
desde el catálogo del propio desafío, ver 2.4) y devuelve un único `ScoreBreakdown` con todo.

**Por qué:** el único lugar del sistema que necesita combinar varias `ScoringRule` es este
método. Un `CompositeScoringRule` que envolviera la lista antes de dársela a `ChallengeSpec`
no evitaba ninguna duplicación real, porque no había un segundo consumidor que repitiera esa
lógica — y de hecho `ChallengeSpec.score` ya tenía que concatenar manualmente la salida de esa
regla con la de `PenaltyScoringRule` para separar el catálogo de penalizaciones (2.4). Sacar el
Composite intermedio deja un solo mecanismo de combinación en vez de dos.

**Alternativas descartadas:** ver 5.10 para la que se aplicaba antes.

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

### 2.3.1 La naturaleza de una contribución es un concepto del dominio

**Patrón / principio:** Dependency Inversion (DIP), evitar type codes.

**Dónde:** `domain/scoring/ContributionKind` (`EARNED`, `BONUS`, `PENALTY`), usado por
`ScoreBreakdown.totalOf` y consultado por `TeamScoreSummary.penaltyPoints`.

Cada contribución declara qué representa, con independencia de qué regla la produjo. El desempate por
penalizaciones pregunta `breakdown.totalOf(ContributionKind.PENALTY)`.

**Por qué:** el ranking es política de alto nivel y no puede depender de una clase concreta de
`domain.scoring.rule`. Antes preguntaba `totalFor(PenaltyScoringRule.CODE)`, es decir importaba una
implementación y filtraba por el texto de su constante: renombrar esa constante rompía los desempates
en silencio —compilaba, corría y devolvía cero para todos— y un reglamento con dos reglas de
penalización distintas sólo habría contado una. Con `ContributionKind`, el paquete `ranking` no
importa nada de `scoring.rule` y el compilador sostiene la relación.

**Consecuencia adicional:** la publicación puede separar lo ganado de las bonificaciones y de las
penalizaciones sin conocer ninguna regla concreta, que es lo que el requisito de explicabilidad pide.

**Alternativas descartadas:** dejar el código de regla como `String` y seguir comparando textos
(`totalFor("PENALTIE")` compila y devuelve cero sin avisar); que el ranking recibiera la lista de
códigos de penalización desde el reglamento (mueve el acoplamiento de lugar en vez de eliminarlo).

### 2.4 Penalizaciones y bonificaciones como parte del mismo mecanismo

**Patrón / principio:** uniformidad de modelo.

**Dónde:** `PenaltyDefinition`, `IncidentReport`, `PenaltyScoringRule`, `ThresholdBonusRule` y
`ChallengeSpec.penalties`.

Una penalización es una contribución negativa y una bonificación una contribución positiva: ambas
aparecen en el mismo desglose, etiquetadas con su `ContributionKind`.

El catálogo de penalizaciones es un componente de `ChallengeSpec`, no un parámetro escondido dentro
de la regla de puntaje, porque cumple dos funciones: `ChallengeSpec.validateIncidents` rechaza al
capturar un incidente que el reglamento no define, y `ChallengeSpec.score` arma con él la
`PenaltyScoringRule` que aplica las deducciones. Una sola fuente de verdad para las dos cosas.

**Por qué:** el desglose queda completo y auditable en una sola estructura, y el total ya contempla
ajustes sin pasos posteriores.

**Alternativas descartadas:** aplicar las penalizaciones después del cálculo, como un descuento sobre
el total, lo que las dejaría fuera de la explicación y obligaría a un orden implícito de aplicación.

## 3. Reglamento, versionado y recálculo

### 3.1 Reglamento inmutable y versionado

**Patrón / principio:** Value Object inmutable, fábrica estática.

**Dónde:** `domain/rulebook/Rulebook`, `RulebookVersion` y `Rulebook.of`.

Un `Rulebook` reúne los desafíos, la política de elegibilidad y los criterios de desempate de una
versión. Publicar un reglamento nunca modifica el anterior: `PublishRulebookUseCase` crea la versión
siguiente y `Competition.activateRulebook` sólo acepta versiones que superen a la vigente.
`PublishRulebookUseCase` ya recibe todos los componentes juntos y los pasa a `Rulebook.of`, que
indexa los desafíos por identificador y construye el reglamento. Si un identificador se repite,
conserva el último desafío recibido, igual que la implementación anterior. El constructor mantiene
las validaciones y las copias defensivas del mapa de desafíos y la lista de desempates.

**Por qué:** el enunciado exige poder recalcular resultados con exactamente la versión de reglas
correspondiente. Eso sólo es confiable si las versiones publicadas son inmutables. La fábrica
conserva esa garantía y evita el estado intermedio del builder, cuyo único consumidor ya tenía
todos los datos disponibles.

**Alternativas descartadas:** un builder para volver a reunir datos que ya llegan juntos; un
reglamento mutable con historial de cambios (cualquier corrección alteraría resultados ya
publicados); guardar sólo la versión vigente (haría imposible el recálculo histórico).

### 3.2 La versión de reglas se fija en la ronda y viaja con el resultado

**Patrón / principio:** Snapshot de configuración.

**Dónde:** `Round.rulebookVersion`, `RunResult.rulebookVersion`, `CategoryScoringService`.

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

La política de revisiones —una fila por revisión, publicar reemplaza la provisional, las anteriores
se conservan— se verifica con `StandingsRepositoryContractTest`. `InMemoryStandingsRepository`
almacena un `TreeMap` por competencia y categoría, con el número de revisión como clave: `put`
reemplaza la misma revisión, `lastEntry` obtiene la vigente y sus valores ya están ordenados para
el historial. Las consultas devuelven listas inmutables copiadas del mapa. El contrato se mantiene
aunque las revisiones se guarden fuera de orden, sin eliminar elementos de una lista ni ordenarla
en cada lectura.

**Alternativas descartadas:** una tabla mutable que se sobrescribe (pierde el histórico y no permite
comparar antes y después de una apelación); publicar automáticamente tras generar (impide revisar el
resultado provisional); dejar la regla de upsert implícita en cada adaptador, que hace que dos
implementaciones del mismo puerto signifiquen cosas distintas.

### 3.4 Desempates como cadena de comparadores configurable

**Patrón / principio:** Strategy + composición de `Comparator`.

**Dónde:** `domain/ranking/TiebreakRule` (extiende `Comparator<TeamScoreSummary>`), sus
implementaciones en `domain/ranking/rule` y `RankingService`.

`RankingService` arma el comparador final: puntaje total descendente y luego, en orden, cada regla de
desempate del reglamento. Si ninguna regla separa a dos equipos, comparten posición y la siguiente
posición salta, y cada entrada registra qué regla resolvió el desempate.

Cada entrada registra un `AppliedTiebreak`, que lleva el código del criterio —para ordenar o filtrar
por máquina— y su descripción, que es el texto que ve un juez. Guardar sólo el código no alcanzaba:
dos `FastestMetricTiebreak` configurados con métricas distintas producen el mismo `FASTEST_METRIC` y
la tabla no decía cuál criterio había resuelto el empate.

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

Que un turno caiga dentro de las fechas de la competencia es una invariante distinta y vive donde
están esas fechas: `Competition.requireDateWithinPeriod`. El caso de uso la invoca por cada turno, de
inicio y de fin. Un tipo de conflicto es un `ScheduleConflictType`, no un `String`.

**Alternativas descartadas:** poner la validación en el caso de uso (mezcla orquestación con negocio y
no se puede reutilizar); poner la validación en `Round` (no ve los turnos de las demás rondas); dejar
el período de la competencia como dato decorativo, que es lo que permitía agendar un turno en 2027
dentro de una competencia de cuatro días de marzo de 2026.

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

**Patrón / principio:** DIP, bitácora de auditoría mediante un puerto explícito.

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

El caso de uso valida **antes** de mutar: resuelve la corrección contra el desafío y recién después
acepta o rechaza la apelación. Si validara después, una corrección inadmisible dejaría la apelación
aceptada y la corrección sin aplicar, dos estados incompatibles en el mismo flujo. `TeamRegistration`
protege sus transiciones con el mismo criterio que `Appeal`: una inscripción se decide una sola vez.

**Alternativas descartadas:** un campo de estado editable desde afuera (cualquier código podría dejar
la apelación en un estado inconsistente); mutar primero y validar después confiando en que la
ausencia de `save()` alcanza para descartar el cambio, que sólo es cierto mientras el adaptador no
devuelva la instancia viva que acaba de mutarse.

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

Los once identificadores repiten el mismo cuerpo de cuatro líneas. **Es duplicación deliberada.**
Los records nominales hacen explícitos los tipos `CategoryId` y `TeamId` en las firmas y evitan
confundirlos. Un `Id<T>` correctamente diseñado también podría preservar esa distinción; se
prefirieron tipos concretos por su legibilidad y simplicidad en este módulo.

También son value objects tipados `ScoringRuleCode` y `ScheduleConflictType`, por la misma razón:
`totalFor("PENALTIE")` compilaba y devolvía cero.

**Alternativas descartadas:** `UUID`/`String` para todos los ids (intercambiables por error); `double`
para puntajes (errores de redondeo inaceptables en un resultado deportivo); una jerarquía de
identificadores para ahorrar la repetición, que destruye la garantía que justifica tenerlos.

### 4.7 Tiempo e identificadores inyectados

**Patrón / principio:** DIP aplicado a dependencias ambientales.

**Dónde:** `java.time.Clock` e `IdGenerator` inyectados en los casos de uso;
`infrastructure/id/SequentialIdGenerator`.

`IdGenerator` declara un método por tipo de identificador (`nextTeamId()`, `nextRunId()`, …) en lugar
de un `nextId(String prefix)` genérico. Con el prefijo como texto, `RunId.of(idGenerator.nextId("TEAM"))`
compilaba y producía un identificador válido y equivocado, y el formato quedaba repartido por ocho
casos de uso; ahora vive entero en el adaptador.

**Por qué:** ningún caso de uso llama a `Instant.now()` ni genera ids por su cuenta, así que los tests
corren con un reloj fijo y con identificadores predecibles, y las aserciones sobre marcas de tiempo
no dependen del momento de ejecución.

**Alternativas descartadas:** `Instant.now()` y `UUID.randomUUID()` directos, que vuelven los tests no
determinísticos.

### 4.8 Reutilización del cálculo entre generar y recalcular

**Patrón / principio:** DRY, servicio de aplicación.

**Dónde:** `application/service/CategoryScoringService`.

Recorre las rondas de una categoría, puntúa cada corrida con su reglamento fijado y arma los
`TeamScoreSummary` que consume `RankingService`. Lo usan `GenerateStandings`, `RecalculateStandings` y
`CalculateRunScore`. Se llama *scoring service* y no *collector* porque lo que hace es puntuar: el
nombre anterior describía el bucle, no la responsabilidad.

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

No se aplicó: el cableado es manual en `RoboLeagueCompositionRoot`.

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
sólo lo que el negocio usa (ISP). Lo que sí se comparte entre adaptadores es el **test de contrato**,
que es donde la repetición sí sería peligrosa.

### 5.9 Métodos en las interfaces "por si acaso"

No se aplicó: una interfaz sólo declara lo que algún cliente llama. `ScoringRule` y `EligibilityRule`
tenían un `code()` que ninguna clase invocaba nunca —dieciséis implementaciones existiendo para
llenar un contrato sin usuarios— y se eliminó; las constantes `CODE` quedaron, porque etiquetar
contribuciones y violaciones sí es un uso real. `TiebreakRule.description()` tampoco tenía cliente:
en vez de borrarlo se le dio el que le faltaba, `AppliedTiebreak`, porque resolvía un agujero real de
explicabilidad.

**Consecuencia:** una regla de puntaje ya no puede identificarse a sí misma de forma polimórfica. Si
alguna vez hace falta —por ejemplo, para desactivar reglas por configuración— habrá que volver a
agregar el método, esta vez con un cliente que lo justifique.

### 5.7 Motor de reglas configurable por datos

No se aplicó: las reglas de puntaje se componen en código Java, no se interpretan desde una
configuración externa o un DSL.

**Consecuencia:** publicar un reglamento con una fórmula inédita requiere una clase nueva y un
despliegue, no un archivo de configuración. A cambio, las reglas son tipadas, testeables y depurables;
un intérprete propio hubiera sido la parte más riesgosa del sistema. La interfaz `ScoringRule` deja la
puerta abierta a agregar un adaptador que construya reglas desde datos.

### 5.10 Composite explícito para combinar reglas de puntaje

Se aplicó en un primer momento (`CompositeScoringRule`, ver versión anterior de 2.2) y se sacó
después de la refactorización de 15b77aa: `ChallengeSpec` pasó a separar el catálogo de
penalizaciones de la regla de puntaje principal (S2 en `REVIEW.md`), y desde ese momento
`ChallengeSpec.score` ya combinaba dos fuentes de contribuciones a mano (`Stream.concat`)
además de la que el Composite armaba. Mantener las dos formas de combinar reglas —una envuelta
en una clase, otra manual— era la inconsistencia real, no el Composite en sí. Se resolvió
haciendo que `ChallengeSpec` sostenga la lista de reglas directamente.

**Consecuencia:** se pierde la posibilidad de anidar composites dentro de otros composites (un
bloque de puntaje que agrupa a su vez otros bloques con su propio nombre). Ningún desafío de
este dominio necesitó esa profundidad: una lista plana alcanza para expresar "una combinación
de factores" tal como lo pide el enunciado. Si en el futuro un reglamento necesitara agrupar un
subconjunto de reglas bajo un nombre propio, nada impide reintroducir una implementación de
`ScoringRule` que envuelva una sublista — la interfaz no cambió, sólo dejó de ser obligatorio
pasar por ella para combinar las reglas de un desafío.

### 5.8 Persistencia real, API REST y seguridad

Fuera del alcance de esta entrega según la consigna. Los adaptadores en memoria existen para poder
ejecutar y probar el dominio de punta a punta.

**Consecuencia:** no hay transaccionalidad ni concurrencia global: un caso de uso que escribe en
varios repositorios no es atómico. Los puertos existentes permiten sustituir el almacenamiento,
pero una integración real también deberá resolver mapeo, transacciones, concurrencia y fallos.

## 6. Cobertura de los requisitos obligatorios

| Capacidad | Dónde se resuelve |
| --- | --- |
| Configuración del evento | `CreateSeasonUseCase`, `CreateCompetitionUseCase`, `Season`, `Competition`, `Category` |
| Registro de equipos | `RegisterTeamUseCase`, `TeamRegistration`, `Member`, `Robot`, `TeamDocument` |
| Elegibilidad | `EligibilityPolicy` y las reglas de `domain/eligibility/rule` |
| Configuración de desafíos | `ChallengeSpec`, `MetricDefinition`, `domain/scoring/rule/*`, `PenaltyDefinition` |
| Programación | `ScheduleRoundUseCase`, `Round`, `Heat`, `TimeSlot`, `ScheduleConflictDetector`, `Competition.requireDateWithinPeriod` |
| Captura de resultados | `CaptureRunResultUseCase`, `RunResult`, `MeasurementSet`, `JudgeEvaluation`, `IncidentReport` |
| Cálculo explicable | `CalculateRunScoreUseCase`, `ScoreBreakdown`, `ScoreContribution`, `ContributionKind` |
| Ranking | `RankingService`, `TiebreakRule`, `AppliedTiebreak` y sus implementaciones |
| Publicación | `Standings`, `PublicationStatus`, `GenerateStandingsUseCase`, `PublishStandingsUseCase`, `GetStandingsUseCase` |
| Apelaciones | `Appeal`, `SubmitAppealUseCase`, `ResolveAppealUseCase` |
| Recálculo | `RecalculateStandingsUseCase`, `CategoryScoringService` |
| Auditoría | `RunResult.corrections()`, `Standings.revision()`, `AuditLog`, `FindAuditTrailUseCase` |

## 7. Estrategia de pruebas

Los tests unitarios cubren las reglas donde vive el negocio: cálculo de cada criterio de puntaje y su
composición, validación de mediciones contra el desafío, elegibilidad, desempates y posiciones
compartidas, conflictos de agenda, historial de correcciones y transiciones de apelaciones y
publicación.

Dos tests parametrizados recorren las siete reglas simples enumeradas en `ScoringRulesTest.everyRule`
y verifican el contrato común (ver 2.1.1). Una implementación nueva debe incorporarse a ese proveedor
y tener pruebas de su fórmula; no existe descubrimiento automático de clases. El compuesto tiene
pruebas específicas.

Los tests de integración ejercitan los casos de uso contra los adaptadores en memoria a través del
composition root, con un reloj fijo e identificadores secuenciales. Cubren los escenarios de negocio
más relevantes: aceptar y rechazar una inscripción, programar una ronda con conflictos o fuera del
período de la competencia, capturar resultados con validaciones de mediciones y de incidentes,
obtener el desglose explicable, sostener la versión de reglamento fijada al capturar, publicar
posiciones y el circuito completo de apelación aceptada, corrección y recálculo que reordena la
tabla, incluido el caso en que la corrección se rechaza y la apelación queda sin resolver.

`StandingsRepositoryContractTest` fija el contrato del puerto de posiciones; el adaptador en memoria
lo hereda y cualquier adaptador futuro también.

El reglamento sobre el que corren los tests es `support/RescueEditionFixture`, propio de `src/test`.
El recorrido de ejemplo de `demo` arma el suyo por separado: las pruebas del dominio y de casos de
uso no dependen de su configuración. `DemoScenarioTest` sí verifica intencionalmente el demo.

## 8. Evolución: qué cambia cuando cambia una dependencia o una regla

**Criterio:** identificar cambios plausibles y localizar su impacto. No es posible garantizar que
cualquier requisito futuro se resuelva sin modificar el dominio. Se preserva el núcleo cuando el
cambio es tecnológico o de representación; un cambio semántico se modela donde corresponde.
No se crean interfaces ni integraciones sin un cliente real.

Actualmente no existe una API HTTP ni una API externa consumida. Los puertos de entrada son la API
Java del módulo. Las siguientes fronteras describen cómo integrar esas tecnologías cuando haya un
requisito concreto, no componentes HTTP ya implementados.

| Cambio | Punto de adaptación | Condición y prueba necesaria |
| --- | --- | --- |
| Cambia una URL, un campo JSON o un código HTTP de nuestra futura API | Controller, DTO y mapper de entrada/salida | El mismo significado se traduce al mismo `Command`; probar formato y errores en el adaptador |
| Se sustituye REST por otra entrada, como mensajería | Nuevo adaptador que invoca los mismos puertos de entrada | Un transporte asíncrono también exige decidir duplicados, orden e idempotencia |
| Cambia la API de un proveedor externo | Adaptador detrás de un puerto de salida definido por la necesidad de aplicación | Traducir datos, unidades y errores; probar contrato e integración con el proveedor |
| Se reemplaza el proveedor completo | Nueva implementación del mismo puerto y nuevo ensamblado | Sólo es sustituible si conserva el contrato semántico; una capacidad faltante exige una decisión del negocio |
| Memoria se reemplaza por SQL u otro almacenamiento | Adaptadores de repositorio y composition root | Preservar versiones/revisiones, ausencia y orden; reutilizar tests de contrato y agregar integración real |
| Cambia reloj o formato de identificadores | `Clock` o implementación de `IdGenerator` | No depender del texto de IDs en reglas; usar reloj controlado en pruebas |
| Cambia un coeficiente o umbral | Configuración de una nueva versión del reglamento | Probar resultado esperado y conservación del cálculo anterior |
| Aparece una fórmula nueva | Nueva `ScoringRule`, configuración y pruebas | Conservar contribuciones explicadas, tipos y comportamiento con datos ausentes |
| Aparece una restricción o desempate | Nueva `EligibilityRule` o `TiebreakRule` | Verificar composición, prioridad y contratos |
| Se exige tomar sólo el mejor intento | Política de agregación en lugar de la suma fija de `TeamScoreSummary` | Cambio de negocio aún no configurable; probar escenarios donde suma y mejor intento producen ganadores distintos |
| Cambian permisos, plazos o etapas de apelación | Reglas, casos de uso y, si corresponde, estados de dominio | Probar transiciones permitidas y prohibidas; el DTO HTTP no decide estas políticas |

Una API de entrada debería recorrer `HTTP DTO → mapper → Command → caso de uso` y mapear la
respuesta a un DTO propio. Serializar entidades mutables como contrato público acoplaría la API
al modelo interno. El mapper traduce representación; fórmulas, elegibilidad y transiciones quedan
en dominio. Cambiar `elapsed_ms` por `time_seconds` requiere convertir unidades, no sólo renombrar.

Para un proveedor externo de captura, un futuro puerto podría expresar «obtener las mediciones de
una corrida» usando tipos del módulo. URL, autenticación, SDK y respuestas del proveedor vivirían
en el adaptador. Ese puerto todavía no existe y se definirá según una necesidad concreta. Si el
proveedor nuevo sólo entrega un puntaje final y el negocio exige mediciones para explicar y
recalcular, ningún mapper puede inventarlas: esa sustitución requiere revisar capacidad o requisito.

Un timeout de escritura puede ocurrir después de que el proveedor haya aceptado la operación.
Reintentar sin una política de idempotencia puede duplicarla. Los casos de uso actuales son
sincrónicos y no ofrecen esa garantía: incorporar red requiere modelar resultados y límites,
además de implementar transporte. Guardar apelación, corregir corrida y auditar tampoco es hoy
una transacción; incluso una validación tardía del actor puede fallar después de mutar.

La reproducción histórica depende de conservar las versiones: `RulebookRepository.save` permite
reemplazar una existente, aunque el flujo normal publique una nueva. Una persistencia que exija
versiones inviolables debe fortalecer ese contrato. Las futuras reglas deben mantener inmutable
su configuración; `record` y copias de listas no fuerzan inmutabilidad profunda de una estrategia.
Versionar parámetros tampoco congela el código ejecutable: cambiar el algoritmo de una clase de
regla podría alterar resultados históricos que la usen. Una evolución durable debe conservar la
semántica antigua, por ejemplo mediante estrategias versionadas, y probar resultados históricos
conocidos al modificar fórmulas, precisión o redondeo.

**Evidencia de evolución:** `RulebookEvolutionTest` incorpora una estrategia definida sólo en tests,
la combina con reglas existentes y publica reglamentos sucesivos. Comprueba que una ronda anterior
siga validándose y puntuándose con su versión, aunque la siguiente exija otra métrica.
`AppealRecalculationTest` cambia fórmula y desempates en un reglamento nuevo y verifica que el
recálculo anterior conserve sus reglas. Son pruebas de puntos de extensión concretos; no demuestran
compatibilidad con un proveedor HTTP todavía inexistente.

## 9. Pruebas: caminos exitosos, rechazos esperados y errores

Cada capacidad debe tener un escenario exitoso que compruebe el resultado de negocio, además de
rechazos relevantes y límites. Rechazar una inscripción inelegible es un resultado esperado con
estado y motivos; no todo camino alternativo debe lanzar una excepción.

| Capacidad | Camino exitoso | Alternativa o error cubierto | Pruebas |
| --- | --- | --- | --- |
| Configurar evento | Temporada, competencia, varias categorías, fechas límite y auditoría | Año incoherente, fechas fuera de temporada, temporada inexistente | `EventConfigurationUseCaseTest` |
| Evolucionar reglamento | Versiones nuevas y cálculo histórico conservado | Sin desafíos o competencia inexistente | `RulebookEvolutionTest` |
| Registrar y evaluar | Equipo aceptado y guardado | Rechazo guardado con motivos; decisión repetida | `RegisterTeamUseCaseTest`, `EligibilityPolicyTest` |
| Programar | Turnos normales, consecutivos y simultáneos con recursos independientes | Conflictos existentes o dentro del comando, fechas inválidas, equipo rechazado | `ScheduleRoundUseCaseTest`, `ScheduleConflictDetectorTest` |
| Capturar | Datos válidos y último intento permitido sin reemplazar el primero | Métrica ausente, intento inválido/repetido, equipo sin turno, incidente desconocido | `CaptureRunResultUseCaseTest`, `ChallengeSpecTest` |
| Puntuar | Fórmulas, bonos, deducciones y suma explicada | Datos ausentes con cero explicado; topes y bono no otorgado | `ScoringRulesTest`, `CalculateRunScoreUseCaseTest` |
| Ordenar | Totales y desempates, incluido tiempo | Empate completo; métrica ausente en uno o ambos equipos | `RankingServiceTest` |
| Publicar posiciones | Provisional a definitiva | Generación y publicación repetidas | `StandingsLifecycleTest`, `StandingsTest` |
| Apelar | Aceptación con corrección y sin corrección | Rechazo, equipo ajeno, corrección inválida, decisión repetida/fecha inválida | `AppealRecalculationTest`, `AppealTest` |
| Recalcular | Nueva revisión y reglas históricas | Revisiones anteriores conservadas aun publicando otro reglamento | `AppealRecalculationTest`, `StandingsRepositoryContractTest` |
| Auditar/conservar | Actor, fecha, acciones, originales y correcciones | Consultas vacías e historiales separados por categoría y competencia | Pruebas de configuración, resultados, apelación y repositorio |

Se comprueban valores, estados y efectos observables. Algunos errores previos a persistir también
verifican conservación del estado: corregir un incidente desconocido permite capturar el mismo
intento; un conflicto dentro del comando no deja reservados los primeros turnos. Eso no implica
atomicidad frente a todos los fallos posteriores.

Revisión de evolución y cobertura del 16 de septiembre de 2026: se incorporaron 25 escenarios;
`mvn test` ejecutó **111 tests, 0 fallos, 0 errores y 0 omitidos**. No se establece una proporción
obligatoria de tests exitosos/negativos ni se equipara cantidad con porcentaje de cobertura.
La suite no mide cobertura de líneas ni prueba todavía HTTP, proveedores, SQL, transacciones o
concurrencia. Esas integraciones tendrán pruebas propias cuando existan.
