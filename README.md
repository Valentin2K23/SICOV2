# SICOV

Aplicación de escritorio en Java 23 y Swing. Los datos se guardan localmente en la carpeta `datos/`.

## Abrir el programa

En Eclipse, actualiza el proyecto con F5 y ejecuta `src/com/sicov/App.java` como **Java Application**.

También puedes abrir una terminal PowerShell en esta carpeta y ejecutar:

```powershell
.\compilar.ps1
.\ejecutar.ps1
```

El programa compilado está en `dist/SICOV.jar`. Puede iniciarse con `java -jar dist/SICOV.jar`.
Requiere Java 23 o posterior. Los datos se buscan junto al proyecto incluso al abrir el JAR desde otra carpeta.
**Esc** y **Regresar** vuelven una pantalla en el historial, incluidos los pasos internos de Categorías.
Por ejemplo: productos → lista de categorías → menú de categorías → pantalla anterior.

## Primer uso

1. En Gestión → Categorías → Configurar categorías, agrega el nombre y selecciona su sección.
2. En Gestión → Compras, registra nombre, precio, código, categoría y unidad. También puedes escribir una categoría nueva directamente.
3. En el catálogo de la derecha, escribe la cantidad a recibir y pulsa Enter. La palomita se marca automáticamente. Desmárcala si ese producto no debe incluirse.
4. Pulsa **Registrar recepción** para guardar los productos marcados bajo un folio de entrada.
5. Regresa a Inicio para vender.

## Ventas y cobro

- Escribe o escanea el código en `C:`. Un lector que funcione como teclado y envíe Enter puede agregarlo directamente.
- `2530*3` agrega tres unidades. Repetir un código acumula su cantidad en la misma fila.
- Al seleccionar una fila, su código pasa a `C:`. El contador inferior cambia la cantidad existente; cero elimina el producto.
- Para kilogramos y litros se solicita una cantidad decimal. Los botones + y − cambian una unidad; la casilla permite escribir decimales.
- Un código desconocido muestra **Producto no encontrado** durante tres segundos, con X para cerrarlo.
- **Buscar mercancía** presenta productos marcados sin código físico y permite mostrar todo el catálogo.
- Los botones de billetes y el campo `$` acumulan efectivo. Se guarda la venta cuando el recibido alcanza o supera el total y se muestra el cambio.
- Dos pulsaciones separadas de Enter, dentro de 0.7 segundos, cobran el importe exacto si hay productos y no se ha registrado efectivo. No deben quedar códigos o importes pendientes de capturar ni estar editándose la cantidad. El Enter enviado al capturar un código no cuenta como parte del atajo.
- Los importes se calculan con decimales exactos y se redondean a centavos.

## Promociones

En Gestión → Descuentos / Promociones se configuran nombre, sección, precio del paquete y productos con sus cantidades.
Admite paquetes de un producto o combinaciones de varios. Se aplican automáticamente tantas veces como permita la venta; las unidades restantes mantienen su precio normal.

Las secciones organizan la consulta del botón % en Ventas. Se pueden editar o desactivar promociones.
Si varias reglas comparten productos, se prioriza el mayor ahorro por paquete y cada unidad participa una sola vez. Esta regla inicial puede ajustarse cuando se defina otra prioridad.

## Reportes, salidas y merma

- Reportes permite consultar entradas, salidas o ambas, con fecha, hora, estado y búsqueda por folio.
- Selecciona varios folios con Ctrl para verlos combinados. Las entradas aparecen primero.
- Las entradas detallan productos recibidos. Las ventas se presentan por categoría.
- Salidas muestra el acumulado de ventas vigentes por categoría y permite registrar merma con producto, cantidad y motivo.
- Se conservan las descripciones, categorías y precios históricos.
- El sistema no limita ventas por existencia. En Categorías puedes consultar una cantidad estimada.

## Categorías y cantidades estimadas

Gestión → Categorías abre dos botones cuadrados: **Configurar categorías** y **Ver categorías**.
El primero permite crear categorías con nombre y sección, o seleccionar una existente para asignarle otra sección.
La tabla indica cuántos productos distintos hay registrados en cada categoría y se actualiza al agregar o reasignar productos.
**Guardar categoría** guarda la sección de una categoría existente o crea una nueva si se escribe otro nombre; después limpia el formulario.
Las secciones disponibles son Repostería (galletas, pan y similares), Dulces, Bebidas, Refrescos, Lácteos,
Salchichonería, Carnes, Tecnología, Farmacia, Higiene, Hogar, Snacks, Botana, Alcohol, Abarrotes, Helados, Tabacos y Juguetes.
Las categorías anteriores y las creadas directamente en Compras se conservan como **Sin sección** hasta asignársela.

**Ver categorías** muestra las categorías creadas. Al seleccionar una se ven sus productos, códigos,
unidades y cantidades estimadas, sin mostrar movimientos, folios ni importes de entradas y salidas.
La estimación parte de cero y suma las recepciones, resta ventas vigentes y merma, y excluye ventas canceladas.
Puede ser negativa si faltan recepciones registradas; no bloquea ventas. Se actualiza al registrar operaciones.
Si un producto cambió de unidad y sus registros mezclan piezas, kilos o litros, aparece **Sin estimación: cambió la unidad**.
Los archivos de categorías antiguos con solo nombre se leen sin perder datos; al guardar se incorpora la columna de sección.

## Cancelaciones

Administradores permite configurar una contraseña local inicial para autorizar cancelaciones. No hay contraseña predeterminada.
Al configurarla debes registrar las tres respuestas: **¿Nombre de mascota?**, **¿Número de casa?** y **¿Año de nacimiento?**.
Todas aceptan texto libre: palabras, números y símbolos. Ninguna puede quedar vacía. No se valida un formato de número o fecha.
Para cambiar la contraseña o las respuestas desde **Configurar / cambiar** se solicita la contraseña actual.
Si la olvidaste, pulsa **Cambiar contraseña**, junto a **Guardar contraseña y respuestas** en la única pestaña **Configurar / cambiar**. Se abre una ventana modal: responde correctamente las tres preguntas y escribe dos veces tu nueva contraseña. La ventana se cierra al guardar el cambio o mediante su X; mientras permanece abierta bloquea la pantalla principal.
La recuperación exige que coincidan las tres respuestas; ignora mayúsculas y espacios al inicio y al final. Conserva los acentos y espacios interiores.
Contraseñas y respuestas se guardan mediante PBKDF2 con una sal independiente para cada una. No se muestra ni se recupera la contraseña anterior.
Las instalaciones antiguas sin respuestas deben configurarlas con su contraseña actual antes de usar la recuperación.
En Reportes se puede cancelar una venta con esa contraseña. Permanece visible como CANCELADA y queda excluida de los acumulados.
La administración completa de usuarios y permisos puede adaptarse al flujo que se defina después.

## Archivos y pruebas

Los archivos activos son `productos.csv`, `categorias.csv`, `promociones.csv`, `compras.csv`, `ventas.csv`,
`movimientos_inventario.csv` (merma) y `configuracion.properties`.
Cada operación se guarda junto con sus detalles en un solo CSV, reemplazado de forma atómica cuando el sistema lo permite.
Las otras clases y archivos de la estructura inicial quedan disponibles para ampliaciones.

`probar.ps1` compila y verifica los cálculos, persistencia, cancelaciones, recuperación obligatoria con tres respuestas y conservación de una venta cuando falla el guardado.
Las pruebas usan carpetas temporales y no agregan datos de demostración al catálogo real.
`tests/com/sicov/VerificarPantallas.java` permite renderizar las pantallas con datos temporales para revisar 720p y 1080p.

No se genera ni imprime ticket en esta versión.

## Catálogo cargado con referencias de OXXO

Se añadieron 50 productos comerciales el 21/10/2026, conservando los productos, categorías y promociones anteriores.
Los códigos internos **900001 a 900050** permiten capturarlos manualmente; no son códigos de barras de los fabricantes.
Todos se venden por pieza/envase. Los precios en MXN se tomaron como referencia del catálogo oficial de promociones de OXXO,
consultado ese día (páginas 1 a 5). Cuando la oferta era por dos unidades, el precio cargado es la mitad del paquete;
ese equivalente no representa el precio normal de una compra individual en OXXO. No se crearon promociones automáticas nuevas.
Las ofertas originales pueden tener condiciones, vigencia y disponibilidad propias.
`datos/referencias_productos_oxxo.json` conserva la página fuente, el importe del paquete y la cantidad usados para cada producto.
No se registraron entradas ficticias: las existencias de estos productos comienzan en cero hasta recibir mercancía en Compras.
Después de una carga externa de CSV, cierra y vuelve a abrir SICOV antes de editar productos para leer los datos nuevos.
