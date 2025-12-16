# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WormBase REST API - A Clojure-based REST API that provides widget and field endpoints for retrieving data from a Datomic database. This powers the official WormBase website (wormbase.org) for biological data about C. elegans and other nematodes.

## Common Commands

### Development Server
```bash
export WB_DB_URI="datomic:ddb://us-east-1/WS298/wormbase"
lein ring server-headless 8130
```

### Testing
```bash
lein test                    # Run all tests
lein test-refresh           # Run tests with file watching
lein test rest-api.classes.gene-test  # Run a specific test namespace
```

### Code Quality
```bash
lein code-qa                # Run linting + tests (before PRs/releases)
lein eastwood               # Linting only
```

### Building
```bash
lein with-profile +datomic-pro,+ddb uberjar  # Create jar file
make clean && make docker/app.jar             # Build for Docker
make build                                    # Build Docker image
make run                                      # Run Docker container locally
```

## Architecture

### Entry Point
- `src/rest_api/main.clj` - Defines the Ring app, Swagger API, and aggregates all routes from entity class modules

### Database Layer
- `src/rest_api/db/main.clj` - Mount-managed Datomic connections (main DB + homology DB)
- Connection URI comes from `WB_DB_URI` environment variable
- Uses pseudoace library for Datomic utilities

### Routing System
- `src/rest_api/routing.clj` - Defines `defroutes` macro and route generation
- Routes are auto-generated from entity class definitions using `RouteSpec` protocol
- Two endpoint types: **widgets** (return multiple fields) and **fields** (return single data points)
- URL pattern: `/rest/{widget|field}/{entity_type}/:id/{endpoint_name}`

### Entity Classes Structure
Each biological entity type (gene, protein, variation, etc.) follows this pattern:
```
src/rest_api/classes/
├── {entity}.clj           # Route definitions using defroutes macro
└── {entity}/
    ├── core.clj           # Shared logic (optional)
    └── widgets/
        ├── overview.clj   # Widget implementations
        ├── references.clj
        └── ...
```

Example route definition (`src/rest_api/classes/gene.clj`):
```clojure
(routing/defroutes
  {:entity-ns "gene"
   :widget {:overview overview/widget
            :expression expression/widget ...}
   :field {:alleles_other variation/alleles-other ...}})
```

### Core Utilities
- `src/rest_api/classes/generic_fields.clj` - Reusable field implementations (remarks, xrefs, taxonomy, etc.)
- `src/rest_api/classes/generic_functions.clj` - Shared helper functions
- `src/rest_api/formatters/object.clj` - `pack-obj` function for consistent entity serialization

### Key Patterns
- Widget functions take a Datomic entity and return `{:data ... :description ...}`
- `pack-obj` standardizes entity references with `:id`, `:label`, `:class`, `:taxonomy`
- Dynamic attribute lookup using `(keyword role "attribute")` pattern for generic field implementations
- Evidence is attached to data using `obj/get-evidence`

### Testing
- Tests in `test/rest_api/classes/` mirror source structure
- `test/rest_api/db_testing.clj` provides `db-lifecycle` fixture for mount state management
- Tests require `WB_DB_URI` to connect to Datomic

## Environment Variables

- `WB_DB_URI` - Datomic database URI (required)
- `SWAGGER_VALIDATOR_URL` - Local swagger validator URL (dev only, defaults to localhost:8002)
