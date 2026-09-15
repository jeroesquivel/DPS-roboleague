# RoboLeague

Módulo de dominio de una plataforma de competencias de robótica: configura temporadas, competencias y
categorías, valida la elegibilidad de los equipos, programa rondas con pistas y jueces, captura los
resultados de cada corrida y calcula puntajes explicables según el reglamento vigente al momento de
correr. Sobre esos puntajes resuelve desempates, publica posiciones provisionales y definitivas,
tramita apelaciones y recalcula la tabla conservando los valores originales y cada modificación.

## Requisitos

- JDK 21 o superior
- Maven 3.9 o superior

## Compilar

```bash
mvn clean compile
```

Para generar el `jar` ejecutable en `target/`:

```bash
mvn clean package
```

## Ejecutar

El ejecutable recorre una edición completa —inscripción, programación, captura, cálculo, publicación,
apelación y recálculo— e imprime el desglose de puntaje y las posiciones en cada etapa.

```bash
mvn exec:java -Dexec.mainClass=com.dps.roboleague.Main
```

O, luego de empaquetar:

```bash
java -jar target/roboleague-1.0-SNAPSHOT.jar
```

## Testear

```bash
mvn test
```

Para correr una sola clase de test:

```bash
mvn test -Dtest=CalculateRunScoreUseCaseTest
```

## Estructura

```
src/main/java/com/dps/roboleague
├── domain           reglas y modelos de negocio
├── application      casos de uso y contratos (puertos de entrada y salida)
├── infrastructure   adaptadores en memoria y composition root
└── demo             recorrido de ejemplo sobre los casos de uso
```

Las dependencias apuntan siempre hacia adentro: `domain` no importa nada de `application` ni de
`infrastructure`, y el composition root expone únicamente puertos de entrada, así que ningún
adaptador puede alcanzar un repositorio por su cuenta.

Las decisiones de diseño están documentadas en [DESIGN.md](DESIGN.md).
