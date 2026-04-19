Components of a Test Case

A well-structured test case serves as a precise set of instructions and data to verify that a specific piece of software works as intended. While formats can vary, a comprehensive test case typically includes the following components:

Test Case Identifier: A unique name or number used to track the test case.

Description or Objective: A clear statement of what the test aims to achieve, such as the confirmation of expected behavior.

Preconditions (Initial State): The necessary setup required before executing the test, such as configuring variables or setting up an object in its initial state.

Test Inputs: The specific data fed into the system. These should ideally be chosen from finite samples of valid ranges, equivalence partitions, boundaries, or illegal regions.

Execution Steps: The exact sequence of events, methods, or transitions provided to the system to carry out the test.

Expected Results: The required output or anticipated state transition dictated by the system's design and requirements.

Actual Results: The real-world output or behavior observed after execution, which is then compared directly with the expected result.

Example of a Well-Constructed Test Case

Based on a state-transition sequence for an "Encounter" module:

Field

Details

Test Case ID

TC-ENC-001

Objective

Verify the state transitions of the game from 'Preparing' to 'Waiting'.

Preconditions

Verify that the game is initially in the 'Preparing' state by checking the class membership of gameStateI.

Execution Steps & Results

Step 1: The player dismisses the quality menu.

Expected Result 1: The game transitions to the 'Waiting' state.

Step 2: Move the player character to an adjacent area.

Expected Result 2: Verify that the game remains in the 'Waiting' state.

Kinds of Test Documentation

Testing efforts generate various types of documentation to plan, execute, and analyze software quality. The primary types include:

1. Test Plans

Documents that outline the overall philosophy, extent of unit testing, resource estimates, and where to acquire test inputs. A common example at the earliest stages is a specific Unit Test Plan.

2. Test Specifications or Sets

The collection of test inputs, previous test products, and boundary definitions used to actually execute the testing phase.

3. Personal Document Sets

Informal documentation and tracking maintained by the individual engineers who are personally responsible for unit testing.

4. Formal Documents

Official project records that incorporate testing methodologies and results, often aggregated from individual documentation.

5. Test Results and Defect Logs

Records that capture the outcomes of the tests and track specific metrics, including:

Time spent on testing

Defect count

Defect type

Source of the defect
