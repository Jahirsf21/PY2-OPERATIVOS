# [Proyecto 2 - Gestor de Procesos y Memoria]

## Integrantes:

| Nombre                   | Carnet     |
| ------------------------ | ---------- |
| Deislher Sanchez Funez   | 2023032794 |
| Natalia Granados Rosales | 2021144286 |

### Estado del proyecto: 1,5

### [Enlace del video](https://drive.google.com/drive/folders/1nqoa5_J_q-H5Awmm3TWqruWyYu13yqGH?usp=sharing)

## Objetivos alcanzados y no alcanzados

| Objetivo                                | Estado    | Descripción                                                                                                                                                            |
| --------------------------------------- | --------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Base del proyecto anterior              | Alcanzado | Se conserva el gestor del proyecto 1 (carga y validación de archivos, CPU con registros y pila, BCP, RAM, disco, archivos con INT 21H) y se le agrega planificación.   |
| Ráfaga y tiempo de llegada              | Alcanzado | La ráfaga es la cantidad de líneas del archivo y cada línea ocupa una posición de memoria. La llegada se edita en la lista de trabajos después de cargar.              |
| Tiempo inicial cero                     | Alcanzado | El reloj simulado empieza en 0 y avanza un segundo por paso para todas las CPU.                                                                                        |
| Configuración de la planificación       | Alcanzado | Ventana de configuración para elegir el algoritmo y sus parámetros: quantum, quantum por nivel y límite de espera de MLFQ, tasas A y B de SRR.                         |
| Configuración de memorias               | Alcanzado | Tamaño de RAM, disco y memoria virtual, tipo de asignación y sus parámetros: particiones, tamaño de página y marcos por proceso.                                       |
| Algoritmos de CPU                       | Alcanzado | FCFS, SRT, SPN, RR, HRRN, Selfish Round Robin, Lottery, Por Prioridad, Guaranteed Scheduling y Multilevel Feedback Queue.                                              |
| Asignación de memoria                   | Alcanzado | Particiones fijas iguales y desiguales, asignación dinámica First, Next y Best Fit, paginación física y paginación virtual por demanda.                                |
| Memoria virtual                         | Alcanzado | Paginación virtual con marcos fijos por proceso y reemplazo FIFO; la página que sale de RAM se guarda en la zona de memoria virtual del disco y se recupera al fallar. |
| Hasta 5 CPU                             | Alcanzado | De 1 a 5 CPU elegidas antes de ejecutar, cada una con hasta 5 procesos y afinidad fija; un BCP por CPU con selector.                                                   |
| Finalización y liberación               | Alcanzado | Al terminar, el proceso pasa a TERMINADO y libera su BCP, su memoria (partición, hueco o marcos) y sus páginas en la memoria virtual.                                  |
| Ejecución automática y manual           | Alcanzado | Ejecución segundo a segundo con el Timer o paso a paso.                                                                                                                |
| Interrupciones sin ENTER y sin teclado  | Alcanzado | Se retiró INT 09H; INT 10H, INT 20H e INT 21H se atienden sin esperar ENTER.                                                                                           |
| Un segundo por instrucción              | Alcanzado | Cada instrucción dura un segundo; la tabla de ejecución muestra por CPU qué proceso ejecutó en cada segundo y el ocio.                                                 |
| Estados y cambios de contexto           | Alcanzado | Listas de trabajos y procesos con sus estados (NUEVO, LISTO, EJECUTANDO, BLOQUEADO, TERMINADO) y el BCP de cada CPU con registros, pila y tabla de páginas.            |
| Estadísticas por ejecución y acumuladas | Alcanzado | Por cada simulación: algoritmo, inicio, final, ocio, Tr y Tr/Ts por proceso; las simulaciones se acumulan en un historial hasta Limpiar.                               |
