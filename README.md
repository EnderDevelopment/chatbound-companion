# Minecraft Companion Plugin

Chatbound Companion is a Minecraft plugin that adds a chat-responsive villager companion to a Paper server. This Minecraft companion plugin lets players summon a friend, ask it to follow, stay, wander nearby, or come to them, and get simple scripted chat replies.

## Install

1. Use a Paper server compatible with the Paper 1.21.11 API and run it with Java 21.
2. Build the project with Maven: `mvn package`.
3. Copy `target/plugin.jar` into the server’s `plugins` folder.
4. Restart the server.

## Use

1. In game, run `/friend summon` to spawn your companion.
2. Use `/friend follow`, `/friend stay`, or `/friend wander` to change its behavior. `/friend come` brings it to you, and `/friend remove` removes it.
3. While your companion is present, chat with it. It recognizes phrases about following, staying or waiting, wandering or exploring, coming or being here, greetings, and mining, wood, or building. Other messages receive a simple preset reply.
4. Use `/friend` to see the command prompt. `/aifriend` is an alias.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/ac199302-91ef-40c4-aedb-c6ea71e4276d?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=ac199302-91ef-40c4-aedb-c6ea71e4276d
