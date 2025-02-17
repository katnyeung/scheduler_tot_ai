# Experimental New Era of Programming

Welcome to our experimental project—an exploration of the future of programming where advanced AI reasoning meets persistent vector memory. This repository demonstrates how to rethink traditional decision-making with a novel Tree of Thought (ToT) approach, integrating Spring AI, vector databases, and autonomous scheduling.

## Overview

In our new era of programming, we’re challenging conventional ideas by building systems that can:
- **Dynamically schedule tasks:** Trigger actions based on real-world events and timing.
- **Incorporate deep reasoning:** Leverage a Tree of Thought (ToT) framework to decompose complex decisions into a series of interconnected, logical steps.
- **Persist context & reasoning:** Use vector databases to store decision nodes as semantic units, complete with context and inter-node relationships.
- **Refine autonomously:** Allow the system to learn from outcomes and automatically refine the reasoning tree for improved future decisions.

## Core Concepts

### Spring AI
Spring AI is used as the brain of the system, orchestrating interactions with our LLM to:
- Evaluate current decision nodes.
- Formulate reasoning steps.
- Guide transitions between stages of the reasoning tree.

### Vector Database
Our vector database (using options like Chroma or PGVector) stores each node in the Tree of Thought as a document containing:
- **Node ID and Type:** Indicating whether the node represents a decision, action, or refinement.
- **Prompt & Context:** The natural language context that the LLM will use.
- **Relationships:** Child nodes are linked via metadata, allowing the entire reasoning tree to be reconstructed and interpreted.

### Scheduler
A scheduling component triggers processes at appropriate times or events. For instance, a daily market open check might initiate the retrieval of the current ToT, prompting the decision engine to:
- Verify conditions (e.g., a stock price threshold).
- Determine whether to execute an action (like sending an alert).

### Tree of Thought (ToT)
At the heart of our project lies the Tree of Thought:
- **Purpose:** Decompose a complex decision into a structured, hierarchical tree.
- **Structure:** Each node represents a logical query, decision, or action. The tree branches out into possible next steps (e.g., “left” for triggering an alert, “right” for holding off).
- **LLM Integration:** The entire tree, along with a prompt for the current context, is provided to the LLM, which recommends the next stage based on learned reasoning.

### Action & Refinement
Once a decision is made:
- **Action Execution:** The corresponding task is triggered—whether that’s sending an email alert, updating data, or logging the event.
- **Refinement:** Feedback is used to update the ToT. The action outcome and any new context are fed back to adjust node relationships, ensuring the system improves over time.

![Untitled diagram-2025-02-17-002123](https://github.com/user-attachments/assets/4e75e6d1-a5c2-4d6a-ac0c-456c4482eb0f)

## Use Case Example: Autonomous Stock Analyst

Imagine a system that monitors the NVDA stock price throughout the trading day:
1. **Scheduling:** At market open, the scheduler fetches the complete ToT from the vector DB.
2. **ToT Decision:** The LLM evaluates the tree of decision nodes such as: "Is the current price below today's low?" The node might have branches for “yes” (trigger an alert) and “no” (wait and check later).
3. **Action:** If the price is lower, the system automatically sends an alert email.
4. **Refinement:** Post-action, the outcome (such as the actual price and market conditions) refines the decision tree, updating thresholds or creating new paths in the ToT.

### Sample Node Representation (Stored in the Vector DB)

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

## Why This Matters

This project embodies a truly experimental approach to programming:
- **Flexibility:** By decoupling decision logic (ToT) from execution, you can easily modify or extend the system in real time.
- **Context Awareness:** With vector storage for node semantics, the AI can consider detailed context in its reasoning.
- **Self-Improvement:** The action/refinement cycle ensures the system learns from each decision, potentially leading to smarter automation over time.

