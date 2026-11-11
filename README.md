# SICOV

Aplicación de escritorio en Java 23 y Swing. Los datos se guardan localmente en la carpeta `datos/`.

## Abrir el programa

En Eclipse, actualiza el proyecto con F5 y ejecuta `src/com/sicov/App.java` como **Java Application**.
Si aparece `NoClassDefFoundError` (por ejemplo, `ContextoAplicacion$1`), detén SICOV y usa **Project → Clean** con **Build Automatically** activado antes de volver a ejecutar. La carpeta `bin/` debe reconstruirse completa: las clases auxiliares generadas también son necesarias.

También puedes abrir una terminal PowerShell en esta carpeta y ejecutar:

```powershell
.\compilar.ps1
.\ejecutar.ps1
```

El programa compilado está en `dist/SICOV.jar`. Puede iniciarse con `java -jar dist/SICOV.jar`.
Requiere Java 23 o posterior. Los datos se buscan junto al proyecto incluso al abrir el JAR desde otra carpeta.
La pantalla de venta muestra los productos a la izquierda y el total y el botón **Pagar** a la derecha, con una paleta verde, crema y rojo. El encabezado incluye el teléfono de soporte **5525972795** y un botón **Gestión** que despliega el menú lateral. Al elegir una sección se cierra el menú; **Volver a ventas** aparece al visitar otras secciones. La venta vacía muestra un carrito tenue detrás de los mensajes de bienvenida.
**Esc** y **Regresar** vuelven una pantalla en el historial, incluidos los pasos internos de Categorías.
Si el menú lateral está abierto, **Esc** lo cierra primero.
Por ejemplo: productos → lista de categorías → menú de categorías → pantalla anterior.

## Primer uso

1. En Gestión → Categorías → Configurar categorías, agrega el nombre y selecciona su sección.
2. En Gestión → Compras, registra nombre, precio, categoría y unidad. La categoría se selecciona de las creadas en Gestión → Categorías. Escanea o escribe el código de barras; si el producto no tiene, marca **Sin código físico** y deja el código vacío: se asigna un identificador interno único, como **INT-000001**, al guardar.
3. En la tarjeta de recepción de la derecha, busca un producto y escribe la cantidad recibida en su unidad (pza, kg o L). Pulsa Enter: la palomita se marca automáticamente. Desmárcala si ese producto no debe incluirse. El resumen cuenta todos los seleccionados, aunque estén ocultos por la búsqueda.
4. Pulsa **Registrar recepción** para guardar los productos marcados bajo un folio de entrada.
5. Regresa a Inicio para vender.

## Ventas y cobro

- Escribe o escanea el código en `C:`. Un lector que funcione como teclado y envíe Enter puede agregarlo directamente.
- `2530*3` agrega tres unidades. Repetir un código acumula su cantidad en la misma fila.
- El último producto agregado queda seleccionado. La papelera o **Supr/Delete** lo retira; **Deshacer** permite recuperarlo. **+** y **−** ajustan una pieza, 100 g o 100 ml según la unidad. El editor inferior muestra gramos para productos por kilogramo y mililitros para productos por litro; Enter aplica y vuelve al campo de código.
- Para productos a granel se abre una captura con gramos/kilos o mililitros/litros, botones de cantidades habituales y vista previa del importe. También puedes indicar cuántos pesos quiere llevar el cliente: se conserva la precisión al convertir a la unidad del catálogo y se redondea el cobro a centavos, respetando el importe capturado. Por ejemplo, **$20 de huevo a $38/kg** conserva **$20.00** en el carrito y el ticket. Las etiquetas redondean los gramos a enteros y las demás medidas a tres decimales, y usan **≈** si son aproximadas; el editor junto a + y − muestra un máximo de dos decimales, conservando la cantidad exacta mientras no se edite para que confirmar o pagar no pierda centavos. **Editar cantidad** o doble clic en el carrito abre la misma captura; por pieza ofrece cantidades como 1, 6 o 12.
- **A mano** muestra solo productos a granel (kilogramo o litro): tarjetas con foto o icono en dos columnas, hasta cuatro visibles y ocho al desplazarse. Las tarjetas aprovechan el espacio disponible y se adaptan a ventanas pequeñas. Se ordenan por el número de ventas vigentes que incluyen cada producto, contando una vez por venta, sin sumar peso ni importe; entradas, merma y ventas canceladas no cuentan. Los empates se ordenan por nombre y los productos aún sin ventas quedan al final. El orden se actualiza al registrar o cancelar ventas. Cada botón indica el precio por kilogramo o litro. No cambia la unidad de un producto por su nombre: registra el huevo como pieza o kilogramo según cómo lo vendas.
- Un código desconocido muestra **Producto no encontrado** durante tres segundos, con X para cerrarlo.
- **Buscar productos** o **B** abre el catálogo para buscar por nombre, código o categoría sin distinguir acentos. Hay filtros por categoría, a granel y sin código; ↑/↓ selecciona, Enter agrega y Esc cierra.
- **Pagar** abre la captura del efectivo total del cliente. Los botones de billetes eligen el importe recibido (no lo acumulan); también puedes escribirlo directamente o elegir **Importe exacto**. El cambio se muestra al instante. Confirma con **Confirmar pago y ver ticket** o **Enter** desde cualquier control de la ventana de cobro; solo se acepta si el efectivo cubre el total. Capturar efectivo por sí solo no guarda ni vacía la venta. **Volver a la venta**, Esc o la X conservan los productos y descartan la captura de efectivo.
- El ticket muestra folio, fecha, productos, promociones, total, efectivo y cambio. El carrito permanece visible detrás; cerrar el ticket con su botón, una nueva pulsación de **Enter**, Esc o la X limpia la venta y enfoca el código para el siguiente cliente. El pago se registra una sola vez. Mantener Enter presionado después de pagar no cierra el ticket automáticamente: hay que soltarlo y pulsarlo de nuevo.
- Los atajos de una tecla funcionan en Ventas, fuera de campos de texto o con el campo de código vacío; mientras escribes códigos o cantidades, se respeta la edición. Se retiró el cobro por doble Enter para evitar pagos accidentales.
- Los importes se calculan con decimales exactos y se redondean a centavos.

## Imágenes opcionales de productos

En **Gestión → Compras**, al crear o editar un producto, pulsa **Elegir imagen…**, selecciona un PNG o JPG y guarda el producto. Se muestra una vista previa. **Usar icono de categoría** quita la foto al guardar y recupera el símbolo correspondiente a la categoría; si esa categoría no se reconoce, se usa su sección o el icono general de abarrotes.

Las fotos aparecen en A mano, la búsqueda de productos, el carrito y el catálogo de Compras. A mano conserva su orden por frecuencia de ventas y hasta ocho productos a granel, con imágenes y tarjetas más amplias.

El programa copia las fotos a `datos/imagenes/` y guarda las asociaciones por código en `datos/imagenes_productos.properties`; puedes mover o borrar el archivo que seleccionaste sin perder la copia. Las imágenes importadas se reducen a un máximo de 512 píxeles por lado manteniendo proporción y transparencia. Se admiten PNG/JPG de hasta 20 MB y 25 megapíxeles. Una foto ausente o dañada usa el icono de categoría y permite seguir vendiendo.

El ejemplo **HUEVO (0304)** tiene una foto generada mediante IA en `datos/imagenes/huevo-ia.png`. Su prompt y herramienta se documentan en `datos/imagenes/huevo-ia-prompt.txt`. El catálogo CSV conserva su formato original, sin cambiar precios, unidades ni movimientos.

**JAMÓN (900051)** y **QUESO OAXACA (900052)** están registrados a **$100.00/kg**, sin foto asignada. Puedes añadir sus imágenes desde Compras.

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
Las categorías anteriores sin sección se conservan como **Sin sección** hasta asignársela. Las categorías nuevas se crean en Gestión → Categorías y se eligen de una lista en Compras.

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
En **Gestión → Cancelar ventas** puedes buscar por folio, filtrar ventas vigentes o canceladas y revisar el detalle completo antes de cancelar. Se pide un **motivo obligatorio** y la contraseña de administrador; el botón de cancelación exige una confirmación explícita. También puedes hacerlo desde Reportes con el mismo formulario. La venta permanece en el historial como CANCELADA, conserva sus importes y productos, guarda el motivo y queda excluida de los acumulados y del descuento de cantidades estimadas. Si aún no tienes contraseña, el botón **Configurar contraseña** abre Administradores.
La administración completa de usuarios y permisos puede adaptarse al flujo que se defina después.

## Organización del código

| Ubicación | Responsabilidad |
| --- | --- |
| `src/com/sicov/App.java` | Inicia Swing y localiza la carpeta de datos. |
| `controlador/` | Coordina las operaciones y el estado compartido por las pantallas. |
| `modelo/` | Define productos, unidades, promociones y registros históricos. |
| `servicio/` | Contiene las reglas de ventas, promociones, cobro, inventario y autorización. |
| `persistencia/` | Lee y guarda los CSV, la configuración y las imágenes locales. |
| `vista/` | Construye las pantallas Swing y conecta sus acciones con las operaciones del sistema. |
| `tests/com/sicov/` | Verifica los flujos con carpetas temporales y permite revisar las pantallas. |

El código usa cuatro espacios, sentencias separadas y métodos agrupados por responsabilidad.
`.editorconfig` conserva las reglas básicas de escritura y `config/formato-java.xml` contiene el perfil de formato Java compatible con Eclipse.
Los comentarios explican decisiones que deben conservarse: precisión de importes, cobro único, historial, escritura atómica y manejo de contraseñas.
Las clases reservadas de la estructura inicial están señaladas como tales; los flujos activos se describen en sus clases correspondientes.

## Archivos y pruebas

Los archivos activos son `productos.csv`, `categorias.csv`, `promociones.csv`, `compras.csv`, `ventas.csv`,
`movimientos_inventario.csv` (merma) y `configuracion.properties`.
Cada operación se guarda junto con sus detalles en un solo CSV, reemplazado de forma atómica cuando el sistema lo permite.
Las otras clases y archivos de la estructura inicial quedan disponibles para ampliaciones.

`compilar.ps1` genera las clases en una carpeta temporal limpia, comprueba que no falten dependencias y copia la compilación completa a `bin/` y al JAR.
`probar.ps1` verifica esa misma compilación de `bin/`: cálculos, persistencia, cancelaciones, recuperación obligatoria con tres respuestas y conservación de una venta cuando falla el guardado.
Las pruebas usan carpetas temporales y no agregan datos de demostración al catálogo real.
`tests/com/sicov/VerificarPantallas.java` permite renderizar las pantallas con datos temporales para revisar 720p y 1080p.

Se genera un ticket en pantalla después de confirmar el pago. No se imprime en esta versión.

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
