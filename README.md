# Plataforma de Gestión de Guías de Despacho

API REST desarrollada con Spring Boot para la gestión de guías de despacho de distintas empresas transportistas.

## Objetivo

Desarrollar una plataforma centralizada para la administración de guías de despacho, permitiendo la gestión segura de documentos, el almacenamiento de archivos y la automatización de procesos mediante tecnologías cloud.

## Características

- Gestión de guías de despacho.
- Operaciones CRUD.
- Filtrado de información por distintos criterios.
- Autenticación y autorización mediante JWT.
- Integración con Azure Active Directory.
- Almacenamiento de archivos en Amazon S3.
- Sistema de archivos compartido mediante Amazon EFS.
- Exposición de endpoints mediante AWS API Gateway.
- Despliegue automatizado utilizando GitHub Actions.
- Contenerización mediante Docker.

## Tecnologías Utilizadas

### Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- JWT

### Base de Datos

- Oracle Database (servicio administrado externo a la infraestructura EC2)
- Spring Data JPA
- Hibernate

### Cloud

- AWS EC2
- AWS API Gateway
- Amazon S3
- Amazon EFS

### DevOps

- Docker
- GitHub Actions

### Identidad y Seguridad

- Azure Active Directory

## Arquitectura
```text
                    ┌─────────────────┐
                    │     Cliente     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ AWS API Gateway │
                    └────────┬────────┘
                             │
                             ▼
        ┌─────────────────────────────────────┐
        │ Spring Boot API (Docker en EC2)     │
        └───────┬──────────────┬──────────────┘
                │              │
                │              ▼
                │      ┌──────────────┐
                │      │  Amazon S3   │
                │      └──────────────┘
                │
                ▼
      ┌─────────────────────┐
      │ Oracle Database     │
      │ (externa a EC2)     │
      └─────────────────────┘
                │
                ▼
         ┌──────────────┐
         │ Amazon EFS   │
         └──────────────┘

                ▲
                │
      ┌─────────────────────┐
      │ Azure Active        │
      │ Directory           │
      └─────────────────────┘
```
## Funcionalidades

### Guías de Despacho

- Crear guía de despacho.
- Consultar guía por ID.
- Consultar guías utilizando filtros.
- Actualizar información de una guía.
- Eliminar registros.

### Gestión de Archivos

- Carga de documentos asociados a una guía.
- Almacenamiento seguro en Amazon S3.
- Acceso compartido mediante Amazon EFS.

### Seguridad

- Protección de endpoints mediante JWT.
- Integración con Azure Active Directory.
- Control de acceso basado en autenticación.

## Pipeline CI/CD

El proyecto utiliza GitHub Actions para:

- Compilar automáticamente la aplicación.
- Ejecutar pruebas.
- Construir imágenes Docker.
- Desplegar la aplicación en AWS EC2.

## Aspectos Técnicos Destacados

- Arquitectura basada en servicios REST.
- Autenticación mediante JWT validado contra Azure Active Directory.
- Gestión de archivos utilizando Amazon S3 y Amazon EFS.
- Contenerización de la aplicación mediante Docker.
- Exposición segura de endpoints a través de AWS API Gateway.
- Integración continua y despliegue continuo (CI/CD) mediante GitHub Actions.
- Persistencia de datos utilizando Oracle Database y Spring Data JPA.
- Implementación de validaciones utilizando Jakarta Validation.

## Posibles Mejoras Futuras

- Implementación de auditoría de cambios.
- Incorporación de monitoreo y métricas.
- Generación de reportes y estadísticas.
- Integración con servicios de notificación.

## Autor

Sofía Medina García y Sebastián Tapia
