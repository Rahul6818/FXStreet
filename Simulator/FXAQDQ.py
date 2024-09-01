import numpy as np

# Comments on Input Parameters:
# S0: Initial foreign exchange rate at time 0.
# K: Strike price of the accumulator/decumulator.
# T: Time to maturity in years.
# r_dom: Domestic risk-free interest rate.
# r_for: Foreign risk-free interest rate.
# sigma: Volatility of the FX rate.
# knockout_level: The level at which the product knocks out.
# n_fixings: Number of fixing/ observations dates
# notional: Total notional amount for the entire period.
# option_type: type of option, AQ (for Accumulator) or DQ (for Deccumulator)
# simulations: Number of Monte Carlo simulations for pricing.

def simulate_fx_path(S0, r_dom, r_for, sigma, T, n_fixings, n_simulations, knockout_level):
    dt = T/n_fixings
    paths = np.zeros((n_simulations, n_fixings + 1))
    paths[:, 0] = S0
    knockout = np.zeros(n_simulations)
                            
    for t in range(1, n_fixings + 1):
        z = np.random.standard_normal(n_simulations)
        paths[:, t] = paths[:, t-1] * np.exp((r_dom - r_for - 0.5 * sigma**2) * dt + sigma * np.sqrt(dt) * z)
                                                            
        # Check for knockout condition
        knockout = np.logical_or(knockout, paths[:, t] >= knockout_level)
                                                                                
    return paths, knockout


def price_AccumDecum(S0, K, T, r_dom, r_for, sigma, knockout_level, n_fixings, notional,option_type='AQ', simulations=10000):
    paths, knockout = simulate_fx_path(S0, r_dom, r_for, sigma, T, n_fixings, simulations, knockout_level)
    
    if(option_type == 'AQ'):
        payoff = np.maximum(paths[:, -1] - K, 0)  # Accumulator payoff
    elif(option_type == 'DQ'):
        payoff = np.maximum(K - paths[:, -1], 0)  # Decumulator payoff

    payoff[knockout] = 0  # Set payoff to 0 for knocked-out paths
                            
    discounted_payoff = np.exp(-r_dom * T) * payoff
    return notional * np.mean(discounted_payoff)

# Simulation Parameters
S0 = 1.20  # Initial FX rate (e.g., USD/EUR)
K = 1.25  # Strike price (e.g., USD/EUR)
T = 1  # Time to maturity in years
r_dom = 0.01  # Domestic risk-free interest rate
r_for = 0.02  # Foreign risk-free interest rate
sigma = 0.15  # Volatility of the FX rate
knockout_level = 1.30  # Knockout level of FX rate
notional = 1000000  # Total notional amount
simulations = 10000  # Number of Monte Carlo simulations
# Set the desired fixing frequency here
n_fixings = 12  # Choose DAILY, WEEKLY, BIWEEKLY, or SEMIANNUAL

# Pricing
accumulator_price = price_AccumDecum(S0, K, T, r_dom, r_for, sigma, knockout_level, 12, notional,'AQ', simulations)
decumulator_price = price_AccumDecum(S0, K, T, r_dom, r_for, sigma, knockout_level, 52, notional,'AQ', simulations)

print(f"Accumulator Price: {accumulator_price}")
print(f"Decumulator Price: {decumulator_price}")
