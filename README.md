# Zuffy — your local AI food companion

A complete React frontend and a simple Spring Boot backend, powered by **Ollama
with `llama3.2:3b`**. The assistant can look up real data from this application's
menu and demo orders through Java tools. No OpenAI API key or paid AI credits
are required.

## Start here: one command

1. Install a **JDK 17 or newer**. JDK 21 is recommended; your JDK 22 also works.
2. Install and open [Ollama](https://ollama.com/download).
3. Unzip the project and open Terminal in `zuffy-food-agent`.
4. Run:

   ```bash
   bash start-local.sh
   ```

5. Wait for the backend console to say **Started ZuffyApplication**, then open
   **http://localhost:8081**.

The script starts local Ollama if needed, downloads the configured model if it
is missing, and starts Spring Boot using the included Maven Wrapper. The first
model download is about 2 GB; the first build also downloads Maven dependencies.
Later runs reuse the downloads. Keep this Terminal running. Press Ctrl+C to stop.

**The compiled React UI is already included in Spring Boot. You do not need Node
or a separate frontend server to use the packaged app.**

If Ollama is already running with the model downloaded, start just the backend:

```bash
bash start-backend.sh
```

## What you can do

- Ask for vegetarian meals under a budget; the agent calls `searchMenu` for
  actual demo menu prices and ingredients.
- Get a recipe from ingredients you already have, with follow-up conversation
  context and saved diet preferences.
- Explore and filter the menu, add dishes to a bag, adjust quantities and place
  a demo order.
- Ask where your order is; the agent calls `checkMyOrder` for your browser session.
- Ask about fees or refund behavior; the agent calls `getStorePolicy`.
- See which tools were actually called below the assistant's response.

Try these prompts:

```text
Recommend a vegetarian meal from your menu under ₹200.
I have rice, tomato and onion. Give me an easy dinner recipe.
Can you make that recipe without onion?
What is the status of my latest order?
What are the delivery fees? Does this demo charge money?
```

The menu and orders are sample application data. Checkout collects no money and
does not contact a restaurant. Orders progress through Preparing → On the way →
Delivered over two minutes for demonstration. Order data resets when the backend
restarts. Cart, visible conversation and preferences are saved in this browser.
Session IDs separate demo orders; there is no account system or database.

## Frontend development, if you want to edit React

Install **Node.js 22.12 or newer**. Start the backend in one terminal:

```bash
bash start-backend.sh
```

Then open another Terminal in `zuffy-food-agent`:

```bash
bash start-frontend.sh
```

Open **http://localhost:5173**. Vite proxies `/api` to the Spring Boot backend at
port 8081, so there is no localhost CORS configuration to fight.

After editing React, rebuild the UI included in Spring Boot:

```bash
bash build-web.sh
```

Restart the backend to see the new UI on port 8081.

## Run through IntelliJ

1. Open `spring-boot/pom.xml` as a Maven project.
2. Select a Project SDK of Java 17 or newer (21 or 22 is fine).
3. Under Settings → Build, Execution, Deployment → Build Tools → Maven, choose
   the Maven Wrapper or Bundled Maven, then reload the project.
4. Open `ZuffyApplication.java` and click the green Run button next to `main`.
5. Keep Ollama running and open http://localhost:8081.

There is no need to install the `mvn` terminal command. From `spring-boot`, use:

```bash
sh ./mvnw clean test
sh ./mvnw spring-boot:run
```

The compiler release is explicitly set to Java 17 to avoid the old Java 1.5 /
“release version 5 not supported” error. This project can run on newer JDKs.

## Change the local model

The model must support **tool calling**. To use another installed model:

```bash
OLLAMA_MODEL=qwen2.5:3b bash start-local.sh
```

Or edit `spring-boot/src/main/resources/application.properties`:

```properties
zuffy.model=${OLLAMA_MODEL:llama3.2:3b}
spring.ai.ollama.chat.model=${zuffy.model}
spring.ai.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
```

The default is Llama 3.2 **3B**, not a cloud model. Model accuracy and response
speed depend on the selected model and your computer. A cold first request can
take longer while Ollama loads the model.

## API

All amounts are in Indian rupees.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Backend status and Ollama/model availability |
| GET | `/api/menu` | Eight sample menu items |
| POST | `/api/chat` | Food agent chat |
| POST | `/api/zuffy/chat` | Alias for the same chat handler |
| POST | `/api/recipes/chat` | Compatibility with your previous recipe endpoint |
| POST | `/api/orders` | Create a demo order; server calculates prices and fees |
| GET | `/api/orders?sessionId=...` | List this session's orders |
| GET | `/api/orders/{id}?sessionId=...` | Get one order in this session |

Chat example:

```bash
curl -X POST http://localhost:8081/api/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "Suggest a vegetarian meal under 200 rupees",
    "sessionId": "demo-user-123",
    "history": [],
    "preferences": { "diet": "Vegetarian", "avoid": "" },
    "ingredients": []
  }'
```

The response is JSON with `reply`, `sessionId`, `model`, `toolsUsed` and
`recommendations`. Send the returned `sessionId` and prior user/assistant messages
on follow-up requests. Maximum history is 12 messages; each message may have up
to 4,000 characters. Only `user` and `assistant` history roles are accepted.

Your previous recipe request body still works:

```bash
curl -X POST http://localhost:8081/api/recipes/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "Suggest an easy vegetarian dinner",
    "ingredients": ["rice", "tomato", "onion"],
    "preferences": { "diet": "Vegetarian", "avoid": "" }
  }'
```

Create an order and then ask for its status using the same `sessionId`:

```bash
curl -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "sessionId": "demo-user-123",
    "items": [{ "itemId": "veg-biryani", "quantity": 2 }]
  }'
```

The backend calculates a ₹378 subtotal, ₹29 demo delivery fee and ₹407 total.
Delivery is free for subtotals of ₹399 or more. Client-supplied prices are never
used. Quantities must be 1–10, including duplicate lines of the same item.

## Small backend, clear responsibilities

| File | Responsibility |
|---|---|
| `ZuffyApplication.java` | Starts Spring Boot |
| `ZuffyController.java` | HTTP endpoints |
| `FoodAgentService.java` | System prompt, conversation context, Ollama chat |
| `FoodTools.java` | The three agent tools and per-request tool trace |
| `MenuService.java` | Sample menu and dietary/budget search |
| `OrderService.java` | In-memory demo orders and server-side totals |
| `OllamaStatusService.java` | Model availability check |
| `ApiModels.java` | Small request and response records with validation |
| `ApiExceptionHandler.java` | Readable JSON error responses |

Spring AI's ChatClient sends the user request and tool definitions to Ollama.
If the model requests a tool, Spring AI executes the annotated Java method and
sends its result back to the model. The model then produces the final answer.
The tool trace is built by the executed Java methods, not invented by the model.
Tools are read-only; the explicit checkout button creates demo orders.

Versions: Spring Boot 4.1.1, Spring AI 2.0.1, Maven Wrapper 3.3.4 with Maven 3.9.11,
React 19.3.0, Vite 8.3.3 and the Vite React plugin 6.1.2. Exact frontend versions
are recorded in `package-lock.json`.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `mvn: command not found` | Use `sh ./mvnw ...` inside `spring-boot`, or a root startup script. |
| `Permission denied: ./mvnw` | Run `sh ./mvnw ...`; executable permission is not required this way. |
| “Unable to locate a Java Runtime” | Install a JDK 17+ and reopen Terminal. |
| “Could not connect to localhost:8081” | Start the backend and wait for “Started ZuffyApplication”. `compile` does not start a server. |
| Assistant offline | Open Ollama or run `ollama serve`, then click Refresh in the UI. |
| Model missing | Run `ollama pull llama3.2:3b`. For a custom model, pull that exact name. |
| Port 8081 already in use | Stop your earlier recipe application, or run `PORT=8082 bash start-backend.sh` and open port 8082. For Vite, also use `BACKEND_URL=http://127.0.0.1:8082 bash start-frontend.sh`. |
| Vite port 5173 already in use | Stop the earlier frontend, or simply use the included UI at port 8081. |
| Postman web cannot access localhost | Use Postman Desktop or its Desktop Agent. |
| Maven dependency stays red | Reload Maven in IntelliJ. From `spring-boot`, run `sh ./mvnw -U clean compile`. |
| Cannot download dependencies or model | The first setup needs an internet connection. Check the exact terminal error. |

Check the two servers individually:

```bash
curl http://localhost:11434/api/tags
curl http://localhost:8081/api/health
```

## Tests

From `spring-boot`:

```bash
sh ./mvnw test
```

The integration suite starts the real Spring Boot API and a controlled Ollama
HTTP test server. It verifies the Spring AI tool round-trip, catalog data being
sent back to the model, conversation history, order totals, session separation,
invalid input and the recipe endpoint. It does not download model weights or
claim to test the quality of live model-generated recipes.

For the frontend:

```bash
cd frontend
npm ci
npm run build
```

## Project description you can adapt

Built Zuffy, a React and Spring Boot food assistant powered by local Ollama,
with menu-search and order-tracking tools, conversation context, dietary
preferences and demo checkout with server-calculated pricing.

## Reference documentation

- [Ollama Llama 3.2](https://ollama.com/library/llama3.2)
- [Spring AI Ollama integration](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- [Spring AI tool calling](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Apache Maven Wrapper](https://maven.apache.org/tools/wrapper/)

Component notices and redistribution licenses are included in the project.
