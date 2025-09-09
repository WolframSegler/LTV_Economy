Documentation of OutputCom Fields and JSON Configuration

---

### Industry-level fields

* `occTag`: Determines the OCC category for the industry, used to look up the labor share (RoVC).
* `workerAssignable`: Boolean indicating if the industry can assign workers to outputs.
* `workerAssignableLimit`: Limits the ratio of globally available workers that can be assigned.

### Output-level fields (per commodity)

* `base_prod`: Float. Acts as a scaler when `usesWorkers = true`. Represents daily production in units when used alongside StaticInputsPerUnit. The units inside StaticInputsPerUnit are multiplied with it. Default is always 1.
* `CCMoneyDist` (`constantCapitalMoneySplit` in JSON): Map\<String, Float>. Defines **relative monetary allocation** for constant capital inputs (physical or abstract). Weights are normalized internally. Can be used with or without workers. Mutually exclusive with `StaticInputsPerUnit`. Can include abstract inputs.
* `StaticInputsPerUnit` (`consumptionRequirements` in JSON): Map\<String, Float>. Flat input amounts required per output unit. Scales with market/population if `scaleWithMarketSize = true`. Can include abstract inputs.
* `ifMarketCondsFalse` / `ifMarketCondsTrue`: Lists of conditions affecting output; used to enable/disable production under specific market states.
* `scaleWithMarketSize`: Boolean. If true, output and/or consumption scales proportionally to market size or population.
* `usesWorkers`: Boolean. Determines if labor calculations (RoVC, LPV\_day) are applied.
* `isAbstract`: Boolean. Indicates the output is **not physically produced**; used for accounting and value input only.
* `checkLegality`: Boolean. Used to flag outputs that require legality checks.

### Notes on usage

1. **Production with labor** (`usesWorkers = true`):

   * Workers needed are calculated using OCC and LPV\_day.
   * Total constant capital value `V_cc = P_out * RoCC`.
   * `CCMoneyDist` weights allocate V\_cc to each input.
   * Abstract inputs in `CCMoneyDist` contribute to accounting but have no quantities.
   * Real inputs per unit of output are stored inside `DynamicInputsPerUnit` during initialization.

2. **Non-labor production** (`usesWorkers = false`):

   * Output quantity = `base_prod` × 10^(market_size - 3) (if `scaleWithMarketSize = true`).
   * Consumption requirements from `StaticInputsPerUnit` are scaled accordingly.
   * Inputs are scaled with modifiers from outputs if the output is not abstract.
   * If using `CCMoneyDist` to model inputs instead, the allocations will build a `DynamicInputsPerUnit` map. "If both CCMoneyDist and StaticInputsPerUnit are present, which is undefined, only CCMoneyDist is used. Static inputs are ignored in this case."

3. **Abstract outputs** (`isAbstract = true`):

   * No physical output is generated.
   * Still contributes to value/accounting for RoCC and deficit calculations.

4. **Consumption requirements**:

   * For outputs like `people` or `crew`, `consumptionRequirements` define fixed daily or per-unit consumption of other goods.
   * Quantities are scaled by population size or market size if `scaleWithMarketSize = true`.
   * Abstract consumption entries are accounted for in value but do not produce physical goods.

5. **CCMoneyDist weights**:

   * Do not need to sum to 1; internal normalization ensures that the **proportion of total constant capital** is maintained. It is **strongly advised**, however.
   * Determines monetary allocation per input based on organic composition of capital (OCC) and output value, which is then converted to quantities.

6. **Integration**:

   * The system handles three types of outputs seamlessly:

     * Labor-driven outputs (RoVC + LPV)
     * Non-labor outputs (`base_prod`)
     * Abstract/consumption-only outputs
   * This allows consistent value accounting and production/consumption logic across industries, including mixed abstract and physical inputs.
