# Size

| Name | Size | Prio|
| --- | --- | --- |
| Init Com Server | M | P0 |
| Map Gestion | S | P0 |
| Building Placements | S | P1 |
| Troops Pathfinding | L | P1 |
| Shooting Logic | M | P2 |
| Attack Logic | S | P1 |
| Upgrade Logic | XS | P3 |
| UI | S | P2 |
| Battle Fog | L | P3 |
| Balancing | XS | P3 |
| Ressource Logic | XS | P2 |
| Assets | XS | P0 |
| Building Usage | S | P2 |


# Refinement

## Server Init
- Com TCP/UDP
- Kryo Format (serv)
- Gestion User (serv)
- Gestion Multi-Instance (serv)
- Client Connexion (core)

## Map Gestion
- Generation (serv)
- Transfer de chunk (serv->client)
- ClientView (android-core)

## Asset:
- Export Asset (client)
- Utilitaire sprite sheet (core)
