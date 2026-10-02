# Endless Adventure

A console-based Java RPG course project for CMP 358L at the American University of Sharjah.

## Current status

Development has just started. The planned game combines turn-based combat, character growth, items and equipment, saved progress, and LLM-generated story developments within supported game rules.

## Requirements

- JDK 21
- Eclipse IDE with Java development support, or another Java development environment

The project currently uses only the Java standard library. No API key is needed to run the current version.

## Run in Eclipse

1. Clone this repository or download and extract it.
2. In Eclipse, select **File → Import → General → Existing Projects into Workspace** and choose the repository folder. When using Git, leave **Copy projects into workspace** unchecked so Eclipse works on the same files as the repository.
3. Make sure the project uses a JDK 21 installation for its **JavaSE-21** environment.
4. Open `src/com/endlessadventure/Driver.java`, then select **Run As → Java Application**

The final course submission will include updated run instructions, the requirement-to-code mapping, and any known limitations.

## LLM Development Usage

The following classes are developed using GenAI coding agent:
- com.endlessadventure.llm.LlmClient.java
- com.endlessadventure.llm.LlmRequestException.java

I hereby claim that no other parts of this project is developed by or using GenAI.