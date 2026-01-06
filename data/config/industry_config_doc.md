Documentation of `industry_config.json` Fields

---

### Industry-level fields

* `occTag`: Determines the OCC category for the industry, used to look up the labor share (RoVC).
* `workerAssignable`: Boolean indicating if the industry can assign workers to outputs. Default value is false.
* `ignoreLocalStockpiles`: Boolean. If true, this output’s demand cannot be satisfied from local production or stockpiles and is used to force imports from external markets even if the commodity exists locally. Because production occurs before imports, the inputs can only be supplied through the stockpiles, leaving industries marked with this flag unable to produce. Default value is false.

### Output-level fields (per commodity)

* `baseProd`: Float. Acts as a scaler when `usesWorkers = true`. Represents daily production in units when used alongside InputsPerUnitOutput. The units inside InputsPerUnitOutput are multiplied with it. Default value is 1.
* `target`: Float. Defines an upper limit for the stored amount of this commodity. When target > 0, production (or import) will scale to 0 once the stored quantity exceeds the target, effectively throttling output. A value ≤ 0 disables this limit. Abstract outputs may not have targets. Default value is -1.
* `CCMoneyDist`: Map\<String, Float>. ConstantCapitalMoneyDistribution Defines **relative monetary allocation** for constant capital inputs (physical or abstract). Weights are normalized internally. Can be used with or without workers. Mutually exclusive with `InputsPerUnitOutput`. Can include abstract inputs.
* `InputsPerUnitOutput`: Map\<String, Float>. Flat input amounts required per output unit. Scales with market/population if `scaleWithMarketSize = true`. Can include abstract inputs and work with workers.
* `ifMarketCondsAllFalse` / `ifMarketCondsAllTrue`: Lists of conditions affecting output; used to enable/disable production under specific market conditions.
* `scaleWithMarketSize`: Boolean. If true, output and consumption scales proportionally to market size. Default value is false.
* `marketScaleBase`: Float. Determines the per-market-size scaling factor for this output when `scaleWithMarketSize = true`. Default value is 10.
* `usesWorkers`: Boolean. Determines if labor calculations (RoVC, LPV\_day) are applied. Default value is false.
* `workerAssignableLimit`: Limits the ratio of globally available workers that can be assigned.
* `isAbstract`: Boolean. Indicates the output is **not physically produced**; used for accounting and value input only. Default value is false.
* `checkLegality`: Boolean. Used to flag outputs that require legality checks. Default value is false.
+ `activeDuringBuilding`: Boolean. Marks outputs to be active during construction phase only. Default is false.

### Notes on usage

1. **Production with labor** (`usesWorkers = true`):

   * Every labor-driven output must define `CCMoneyDist`. This is required to calculate the variable capital contribution for each input according to the Labor Theory of Value.
   * Workers needed are calculated using OCC and LPV\_day.
   * Total constant capital value `V_cc = P_out * RoCC`.
   * `CCMoneyDist` weights allocate V\_cc to each input.
   * Abstract inputs in `CCMoneyDist` contribute to accounting but have no quantities.

2. **Non-labor production** (`usesWorkers = false`):

   * Output quantity = `baseProd` × `marketScaleBase`^(market_size - 3) (if `scaleWithMarketSize = true`).
   * Consumption requirements from `InputsPerUnitOutput` are scaled accordingly.
   * Inputs are scaled with modifiers from outputs if the output is not abstract.
   * If both CCMoneyDist and InputsPerUnitOutput are present, which is undefined, only CCMoneyDist is used.

3. **Abstract outputs** (`isAbstract = true`):

   * No physical output is generated.
   * Still contributes to value/accounting for RoCC and deficit calculations.

4. **Inputs per Unit Output**:

   * For outputs like `people` or `crew`, `InputsPerUnitOutput` define fixed daily or per-unit consumption of other goods.
   * Quantities are scaled by population size or market size if `scaleWithMarketSize = true`.
   * Abstract consumption entries are accounted for in value but do not produce physical goods.

5. **CCMoneyDist weights**:

   * Do not need to sum to 1; normalized internally. It is **strongly advised**, however.
   * Determines monetary allocation per input based on organic composition of capital (OCC) and output value, which is then converted to quantities.
