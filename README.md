# Backend de réservation de salles

API REST permettant à une université de gérer ses bâtiments, ses salles, leurs
équipements, ses organisateurs et les réservations associées.

Un utilisateur peut consulter les salles disponibles pour une période, réserver une
salle précise, ou laisser l'application choisir automatiquement la salle la plus
adaptée à son besoin.

Le contrat complet de l'API (endpoints, types, erreurs) est décrit dans
[`openapi.yml`](openapi.yml).

## 1. Prérequis

- Java 21
- Maven (le wrapper `./mvnw` est fourni, aucune installation n'est requise)

Aucune base de données à installer : le projet utilise SQLite, et le schéma est créé
automatiquement par Flyway au démarrage.

## 2. Lancer le projet

```bash
./mvnw spring-boot:run
```

L'application démarre sur <http://localhost:8080> et crée le fichier `base.sqlite`
à la racine du projet. Ce fichier contient un jeu de données d'exemple : deux
bâtiments, trois équipements, trois salles et deux organisateurs.

Pour repartir d'une base vierge, supprimez `base.sqlite` puis relancez l'application.

Pour construire le livrable :

```bash
./mvnw clean package
java -jar target/reservation-0.0.1-SNAPSHOT.jar
```

## 3. Lancer les tests

```bash
./mvnw test
```

Les tests utilisent une base SQLite en mémoire et n'altèrent pas `base.sqlite`.

Pour lancer le linter :

```bash
./mvnw checkstyle:check
```

## 4. Endpoints

### Bâtiments

| Méthode | Route | Description |
| --- | --- | --- |
| `POST` | `/api/buildings` | Créer un bâtiment |
| `GET` | `/api/buildings` | Lister les bâtiments, triés par nom |
| `GET` | `/api/buildings/{buildingId}` | Consulter un bâtiment |
| `PUT` | `/api/buildings/{buildingId}` | Modifier un bâtiment |

### Salles

| Méthode | Route | Description |
| --- | --- | --- |
| `POST` | `/api/rooms` | Créer une salle |
| `GET` | `/api/rooms` | Lister les salles |
| `GET` | `/api/rooms/available` | Rechercher les salles disponibles et compatibles |
| `GET` | `/api/rooms/{roomId}` | Consulter une salle |
| `PUT` | `/api/rooms/{roomId}` | Remplacer les informations d'une salle |
| `PATCH` | `/api/rooms/{roomId}/status` | Placer une salle en maintenance ou la remettre à disposition |
| `PUT` | `/api/rooms/{roomId}/equipment` | Remplacer les équipements d'une salle |

### Équipements et organisateurs

| Méthode | Route | Description |
| --- | --- | --- |
| `POST` | `/api/equipment` | Créer un équipement |
| `GET` | `/api/equipment` | Lister les équipements, triés par code |
| `POST` | `/api/organizers` | Créer un organisateur |
| `GET` | `/api/organizers` | Lister les organisateurs |
| `GET` | `/api/organizers/{organizerId}` | Consulter un organisateur |

### Réservations

| Méthode | Route | Description |
| --- | --- | --- |
| `POST` | `/api/reservations` | Réserver une salle précise |
| `POST` | `/api/reservations/automatic` | Demander l'attribution automatique d'une salle |
| `GET` | `/api/reservations` | Lister et filtrer les réservations |
| `GET` | `/api/reservations/{reservationId}` | Consulter une réservation |
| `PATCH` | `/api/reservations/{reservationId}/cancel` | Annuler une réservation |

## 5. Exemples d'utilisation

Réserver automatiquement une salle pour 25 participants :

```bash
curl -X POST http://localhost:8080/api/reservations/automatic \
  -H 'Content-Type: application/json' \
  -d '{
        "title": "Soutenance de projet",
        "organizerId": 1,
        "start": "2026-10-15T14:00:00+02:00",
        "end": "2026-10-15T16:00:00+02:00",
        "numberOfParticipants": 25
      }'
```

Rechercher les salles libres et équipées d'un vidéoprojecteur :

```bash
curl "http://localhost:8080/api/rooms/available?start=2026-10-15T14:00:00%2B02:00&end=2026-10-15T16:00:00%2B02:00&capacity=10&equipment=PROJECTOR"
```

Annuler une réservation :

```bash
curl -X PATCH http://localhost:8080/api/reservations/1/cancel
```

## 6. Règles métier

### Période de réservation

- Le début doit être strictement antérieur à la fin.
- Le début ne doit pas être dans le passé.
- La durée ne peut pas dépasser huit heures.
- Les dates sont transmises au format ISO 8601 avec fuseau ou décalage UTC, par
  exemple `2026-10-15T14:00:00+02:00`. Elles sont stockées en UTC et renvoyées en UTC.

### Capacité et équipements

Le nombre de participants doit être strictement positif et inférieur ou égal à la
capacité de la salle : une salle de 30 places accueille donc exactement 30 personnes.

La salle doit posséder tous les équipements demandés ; des équipements
supplémentaires sont autorisés. Une liste absente ou vide signifie qu'aucun
équipement particulier n'est exigé.

### Chevauchement

Deux réservations confirmées ne peuvent pas se chevaucher dans la même salle :

```text
reservationExistante.start < nouvelleReservation.end
ET
reservationExistante.end > nouvelleReservation.start
```

Deux réservations consécutives (`10:00–11:00` puis `11:00–12:00`) sont donc
autorisées. Les réservations annulées sont ignorées lors de cette vérification.

### Choix de la salle

Une salle explicitement choisie n'est jamais remplacée automatiquement : la demande
est refusée dès qu'une condition n'est pas satisfaite.

En attribution automatique, le système ne garde que les salles `AVAILABLE`, de
capacité suffisante, possédant tous les équipements demandés et libres sur la
période, puis calcule pour chacune :

- `placesInutilisées = capacité - nombre de participants` ;
- `distance = abs(étageSalle - étageOrganisateur)` dans le même bâtiment ;
- `distance = 10 + abs(étageSalle - étageOrganisateur)` dans des bâtiments différents ;
- `score = distance × 10 + placesInutilisées`.

Le score le plus faible gagne : un étage de distance équivaut à dix places
inutilisées, et changer de bâtiment coûte 100 points. En cas d'égalité, les salles
sont départagées par nom (sans tenir compte de la casse) puis par identifiant, ce qui
rend le résultat déterministe. Si aucune salle n'est compatible, aucune réservation
n'est créée et l'erreur `NO_COMPATIBLE_ROOM` est renvoyée.

## 7. Gestion des erreurs

Toutes les erreurs partagent le même format, produit par `GlobalExceptionHandler` :

```json
{
  "code": "ROOM_ALREADY_RESERVED",
  "message": "La salle Orion est deja reservee sur cette periode",
  "timestamp": "2026-10-01T13:45:12Z",
  "path": "/api/reservations",
  "details": { "roomId": 7, "conflictingReservationId": 38 },
  "fieldErrors": {}
}
```

| Code | HTTP | Signification |
| --- | ---: | --- |
| `VALIDATION_ERROR` | 400 | Un ou plusieurs champs sont invalides |
| `INVALID_RESERVATION_PERIOD` | 400 | Période passée, inversée ou supérieure à huit heures |
| `BUILDING_NOT_FOUND` | 404 | Le bâtiment demandé n'existe pas |
| `ROOM_NOT_FOUND` | 404 | La salle demandée n'existe pas |
| `ORGANIZER_NOT_FOUND` | 404 | L'organisateur demandé n'existe pas |
| `RESERVATION_NOT_FOUND` | 404 | La réservation demandée n'existe pas |
| `EQUIPMENT_NOT_FOUND` | 404 | Au moins un code d'équipement n'existe pas |
| `ROOM_CAPACITY_EXCEEDED` | 409 | La salle choisie est trop petite |
| `MISSING_REQUIRED_EQUIPMENT` | 409 | La salle choisie n'a pas tous les équipements demandés |
| `ROOM_ALREADY_RESERVED` | 409 | Une réservation confirmée chevauche la période |
| `ROOM_UNAVAILABLE` | 409 | La salle choisie est en maintenance |
| `NO_COMPATIBLE_ROOM` | 409 | Aucune salle ne convient à l'attribution automatique |
| `RESERVATION_ALREADY_CANCELLED` | 409 | La réservation est déjà annulée |
| `RESOURCE_ALREADY_EXISTS` | 409 | Un nom, code ou e-mail unique est déjà utilisé |
| `BUILDING_FLOOR_COUNT_CONFLICT` | 409 | Le nombre d'étages est incompatible avec une localisation existante |

## 8. Structure du projet

Le package racine est `reservation`, placé directement sous `src/main` : le `pom.xml`
déclare `<sourceDirectory>src/main</sourceDirectory>` et
`<testSourceDirectory>src/test</testSourceDirectory>`, ce qui évite les niveaux
intermédiaires `java/ismin/cours`.

```text
src/main/reservation/
├── MeetingRoomReservation.java   point d'entrée Spring Boot
├── controller/                   endpoints REST, un contrôleur par ressource
├── dto/                          classes d'échange HTTP, alignées sur openapi.yml
├── exception/                    erreurs métier et handler global
├── model/                        entités JPA
├── repository/                   repositories Spring Data JPA
└── service/                      logique métier

src/main/resources/
├── application.properties
└── db/migration/V1__create_tables.sql   schéma et jeu de données, gérés par Flyway

src/test/reservation/
├── controller/                   tests d'intégration MockMvc
└── service/                      tests unitaires
```

Les services se répartissent les responsabilités ainsi :

- `RoomAssignmentService` : compatibilité des salles, calcul de distance et de score,
  classement. Il ne dépend d'aucun repository et reçoit directement les salles et les
  réservations à considérer, ce qui le rend testable sans persistance.
- `ReservationPeriodValidator` : règles de période (ordre des dates, début dans le
  futur, durée maximale de huit heures).
- `ReservationService` : ordre des contrôles, erreurs renvoyées et persistance.
- `BuildingService`, `RoomService`, `EquipmentService`, `OrganizerService` : CRUD et
  règles propres à chaque ressource.

## 9. Tests

`./mvnw test` exécute 54 tests.

### Tests unitaires

`RoomAssignmentServiceTest` couvre le classement et l'attribution automatique sans
base de données : capacité exactement suffisante, rejet d'une salle trop petite, en
maintenance, déjà réservée ou mal équipée, acceptation de deux réservations
consécutives, calcul de la distance dans un même bâtiment et entre deux bâtiments,
calcul du score, sélection du score le plus faible, départage par nom puis par
identifiant, et absence de salle compatible.

`ReservationPeriodValidatorTest` couvre les règles de période. Ses dates sont
exprimées par rapport à l'instant courant, afin que les tests restent valables quelle
que soit la date d'exécution.

### Tests d'intégration

`RoomControllerTest` et `ReservationControllerTest` démarrent le contexte Spring
complet et appellent l'API via MockMvc : création et consultation d'une salle,
création d'une réservation, attribution automatique, recherche de disponibilité et
vérification de son ordre, refus d'une réservation conflictuelle, annulation puis
nouvelle réservation sur la même période, et format des réponses d'erreur.

Tous les tests suivent la structure GIVEN / WHEN / THEN.

## 10. Choix techniques

- **Identifiants** : les entités utilisent `Long`, conformément au type `int64` du
  contrat. SQLite imposant le type `INTEGER` pour une clé primaire auto-incrémentée,
  la validation de schéma d'Hibernate est désactivée (`ddl-auto=none`) ; le schéma
  reste entièrement décrit et versionné par Flyway.
- **Dates** : les instants sont stockés en UTC (`Instant`) et exposés en UTC au
  format ISO 8601. Le décalage horaire reçu en entrée est donc normalisé.
- **Équipements d'une réservation** : les codes exigés sont conservés tels qu'ils ont
  été demandés, afin qu'une réservation passée reste consultable à l'identique même
  si la salle perd un équipement par la suite.
- **Unicité** : les noms de bâtiment et de salle, les codes d'équipement et les
  adresses e-mail sont uniques sans tenir compte de la casse (collation `NOCASE`).
- **`POST /api/reservations/automatic`** renvoie un `ReservationResponse`, comme
  l'indique le chemin correspondant dans `openapi.yml`. Le schéma
  `AutomaticReservationResponse` y est défini mais n'est référencé par aucun endpoint,
  et `ReservationResponse` interdit les propriétés supplémentaires.
- **Périmètre technique** : le projet s'en tient aux notions vues en cours. Les DTO
  sont des classes Lombok `@Data`, les requêtes de base de données passent par des
  méthodes dérivées de Spring Data (`findByStatus`, `findByBuildingId`) plutôt que par
  du JPQL, et les tris et filtres sont écrits avec des boucles et des `Comparator`.
- **DTO mutualisés** : `BuildingRequest`, `RoomRequest` et `ReservationRequest`
  servent chacun à deux endpoints, les champs attendus étant identiques ou presque.
  `RoomRequest.equipmentCodes` est ignoré par le `PUT`, qui ne modifie pas les
  équipements, et `ReservationRequest.roomId` n'est exigé que par
  `POST /api/reservations` — il est donc vérifié dans le service plutôt que par une
  annotation, afin de rester facultatif pour l'attribution automatique.
- **`GET /api/rooms/available`** vérifie uniquement que `start` précède `end`. Les
  règles « pas dans le passé » et « huit heures maximum » s'appliquent aux
  réservations, pas à une simple consultation.

## 11. Intégration continue

Le workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) s'exécute
automatiquement sur `main` et sur chaque pull request. Il contient deux jobs :

- `lint` : analyse le code avec Checkstyle (`./mvnw checkstyle:check`) ;
- `test` : exécute la suite de tests (`./mvnw test`) et publie les rapports Surefire.
