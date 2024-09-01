import numpy as np
import time

# Comments on Input Parameters:
# S0: Initial foreign exchange rate at time 0.
# K: Strike price of the TRF.
# T: Tenor in year
# r_dom: Domestic risk-freeinterest rate (EUR interest rate).
# r_for: Foreign risk-free interest rate (USD interest rate).
# sigma: Volatility of the FX rate.
# target: Cumulative profit target in pips.
# itm_target: Number of ITM events target.
# n_fixings: Number of observation dates (fixings).
# notional: Notional amount per fixing in domestic currency (EUR).
# leverage: Leverage factor for losses.
# pip_value: Pip value for the FX pair.
# final_payoff_type = EXACT/FULL/None - how much should be accumulated in the last fixing
# simulations: Number of Monte Carlo simulations for pricing.

def simulate_fx_paths(S0, r_dom, r_for, sigma,T, n_fixings, n_simulations):
    dt = T / n_fixings
    paths = np.zeros((n_simulations, n_fixings + 1))
    paths[:, 0] = S0
                    
    for t in range(1, n_fixings + 1):
        z = np.random.standard_normal(n_simulations)
        paths[:, t] = paths[:, t-1] * np.exp((r_dom - r_for - 0.5 * sigma**2) * dt + sigma * np.sqrt(dt) * z)                                        
    return paths

def price_tarf(S0, K, r_dom, r_for, sigma,T, target, itm_target, n_fixings, notional, leverage, simulations, final_payoff_type, tarf_type, target_type):
    start = time.time()
    paths = simulate_fx_paths(S0, r_dom, r_for, sigma,T, n_fixings, simulations)
    dt = T / n_fixings
                
    # Convert pips target to absolute monetary target
    target_amount = target * notional
                            
    total_payoff = np.zeros(simulations)
    
    for sim in range(simulations):
        cumulative_payoff = 0
        accumulated_target = 0 # accumulation which goes into determining the target profit
        itm_count = 0
        for t in range(1, n_fixings + 1):
            if tarf_type == 'buy':
                if paths[sim, t] >= K:
                    payoff = notional * (paths[sim, t] - K)
                    cumulative_payoff += payoff
                    accumulated_target += payoff
                    itm_count += 1
                else:
                    payoff = notional * (paths[sim, t] - K) * leverage
                    cumulative_payoff += payoff
                    # Do not add negative payoff to accumulated_target
            elif tarf_type == 'sell':
                if paths[sim, t] <= K:
                    payoff = notional * (K - paths[sim, t])
                    cumulative_payoff += payoff
                    accumulated_target += payoff
                    itm_count += 1
                else:
                    payoff = notional * (K - paths[sim, t]) * leverage
                    cumulative_payoff += payoff                                                                                 
                    # Do not add negative payoff to accumulated_target
            
            total_payoff[sim] = cumulative_payoff
            if target_type == "PIPS" and accumulated_target >= target_amount:
                if (final_payoff_type == "FULL"):
                    total_payoff[sim] = cumulative_payoff
                elif (final_payoff_type == "EXACT"):
                    total_payoff[sim] = cumulative_payoff - (accumulated_target - target_amount) #min(cumulative_payoff, target_amount)
                elif (final_payoff_type == "NONE"):
                    total_payoff[sim] = cumulative_payoff - payoff                                                       
                break
            elif target_type == "ITM" and itm_count >= itm_target:
                if (final_payoff_type == "FULL"):
                    total_payoff[sim] = cumulative_payoff
                elif (final_payoff_type == "NONE"):
                    total_payoff[sim] = cumulative_payoff - payoff
                break
       # if cumulative_payoff < target_amount and itm_count < itm_target:
       #         total_payoff[sim] = cumulative_payoff
    
    discounted_payoff = np.exp(-r_dom * (n_fixings * dt)) * np.abs(total_payoff)
    end = time.time()
    print ("Total Time", end-start,"seconds")
    return np.mean(discounted_payoff)

# Pricing
# Simulation Parameters
S0 = 1.20  # Initial FX rate (e.g., EUR/USD)
K = 1.25  # Strike price (e.g., EUR/USD)
T = 1 # tenor in year
r_dom = 0.01  # Domestic risk-free interest rate (EUR interest rate)
r_for = 0.02  # Foreign risk-free interest rate (USD interest rate)
sigma = 0.15  # Volatility of the FX rate
target = 10  # Cumulative profit target absolute
itm_target = 20  # Number of ITM events target
n_fixings = 252  # Number of fixings (e.g., daily fixings over a year)
notional = 10000  # Notional amount per fixing in domestic currency (EUR)
leverage = 2  # Leverage factor for losses
#pip_value = 0.0001  # Pip value for EUR/USD
simulations = 10000  # Number of Monte Carlo simulations
final_payoff_type = "FULL"

tarf_buy_price = price_tarf(S0, K, r_dom, r_for, sigma,1, target, 0, n_fixings, notional, leverage,  simulations, 'FULL' ,'buy','PIPS')
tarf_sell_price = price_tarf(S0, K, r_dom, r_for, sigma, 1, 0, itm_target, n_fixings, notional, leverage, simulations, 'FULL','buy','ITM')
print(f"TARF Buy Price: {tarf_buy_price} EUR")
print(f"TARF Sell Price: {tarf_sell_price} EUR")
