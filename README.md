# Experimental New Era of Programming

Welcome to our experimental project—an exploration of the future of programming where advanced AI reasoning meets persistent vector memory. This repository demonstrates how to rethink traditional decision-making with a novel Tree of Thought (ToT) approach, integrating Spring AI, vector databases, and autonomous scheduling.

---

## Overview

In this project, we explore:
- **Scheduling**: Trigger tasks at specific times or events.
- **Tree of Thought (ToT)**: A hierarchical decision-making framework for reasoning through complex tasks.
- **Actions**: Execute tasks based on ToT decisions.
- **Refinement**: Continuously improve the ToT based on outcomes and feedback.

---

## Architecture

The following diagram illustrates the relationship between the Scheduler, Tree of Thought (ToT), Action, and Refinement:

![Untitled diagram-2025-02-17-002123](https://github.com/user-attachments/assets/4e75e6d1-a5c2-4d6a-ac0c-456c4482eb0f)

### Workflow

1. **Scheduler**:
   - Triggers actions at predefined intervals or events.
2. **Action**:
   - Fetches the ToT from the vector database.
   - Evaluates the current state using an LLM.
   - Executes tasks based on ToT decisions.
3. **Refinement**:
   - Updates the ToT based on feedback from executed actions.
   - Writes refined nodes back to the vector database for future use.

---

## Use Case Example: Autonomous Stock Analyst

Imagine a system that monitors the NVDA stock price throughout the trading day:

1. **ToT Creation**:  
   - Analyze critical points to determine the best price to buy NVDA stock.
   - Create a Tree of Thought (ToT) based on multiple criteria, such as price thresholds, market trends, and volatility.
   - Nodes in the ToT represent decisions:
     - **Left Branch**: Leads to "Good," indicating it's a good time to buy.
     - **Right Branch**: Leads to "Hold," meaning conditions are not ideal, and monitoring should continue.

2. **Scheduling**:  
   - Schedule the action to trigger at market open (e.g., 9:30 AM EST).
   - Repeat every 15 minutes during trading hours to evaluate the ToT.

3. **Action**:  
   - Fetch the ToT from the vector database and evaluate it using the LLM.
   - If the ToT returns "Good," send an email alert recommending a buy action.
   - If the ToT returns "Hold," do nothing and wait for the next scheduled evaluation.

4. **Refinement**:  
   - Post-action, analyze the outcome (e.g., actual stock price movements and market conditions).
   - Refine the ToT:
     - Update thresholds or criteria for "Good" and "Hold" decisions.
     - Add new nodes if additional conditions or factors are identified.
   - Save the updated ToT back into the vector database for future evaluations.

---

## Sample Node Representation (Stored in the Vector DB)

{
"content": "Check if NVDA price is below today's low",
"metadata": {
"nodeId": "node_001",
"type": "decision",
"treeId": "nvda_alert_tree",
"children": {
"yes": "node_002",
"no": "node_003"
},
"prompt": "Is the current NVDA price lower than the recorded day's low?"
}
}

text

---

## Why This Matters

This project embodies a truly experimental approach to programming:
- **Flexibility**: By decoupling decision logic (ToT) from execution, you can easily modify or extend the system in real time.
- **Context Awareness**: With vector storage for node semantics, the AI can consider detailed context in its reasoning.
- **Self-Improvement**: The action/refinement cycle ensures the system learns from each decision, potentially leading to smarter automation over time.

