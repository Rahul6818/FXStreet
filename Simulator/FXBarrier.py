import numpy as np

def simulate_fx_double_barrier_option(S0, K, T, rd, rf, sigma, Bu, Bd, option_type='call', barrier_type='knock-out', style='european', n_simulations=10000, n_steps=100):
    """
        Simulate the price of an FX double barrier option using the Monte Carlo method.
        param S0: Current spot price of the underlying asset (e.g., USD/EUR exchange rate)
        param K: Strike price of the option
        param T: Time to maturity (in years)
        param rd: Domestic risk-free interest rate (e.g., USD interest rate)
        param rf: Foreign risk-free interest rate (e.g., EUR interest rate)
        param sigma: Volatility of the underlying asset (annualized)
        param Bu: Upper barrier level
        param Bd: Lower barrier level
        param option_type: 'call' for call option, 'put' for put option
        param barrier_type: 'knock-out' for knock-out option, 'knock-in' for knock-in option
        param style: 'european' for European option, 'american' for American option
        param n_simulations: Number of Monte Carlo simulations
        param n_steps: Number of time steps in each simulation
        return: Simulated option price
    """
    dt = T / n_steps
    discount_factor = np.exp(-rd * T)
    prices = np.zeros((n_simulations, n_steps + 1))
    prices[:, 0] = S0

    for t in range(1, n_steps + 1):
        Z = np.random.standard_normal(n_simulations)
        prices[:, t] = prices[:, t - 1] * np.exp((rd - rf - 0.5 * sigma ** 2) * dt + sigma * np.sqrt(dt) * Z)
                                            
    if barrier_type == 'knock-out':
        if style == 'european':
            barrier_reached = np.any((prices >= Bu) | (prices <= Bd), axis=1)
        elif style == 'american':
            barrier_reached = (prices[:, -1] >= Bu) | (prices[:, -1] <= Bd)
    elif barrier_type == 'knock-in':
        if style == 'european':
            barrier_reached = np.any((prices < Bu) & (prices > Bd), axis=1)            
        elif style == 'american':
            barrier_reached = (prices[:, -1] < Bu) & (prices[:, -1] > Bd)
    else:
        raise ValueError("Invalid barrier type. Use 'knock-out' or 'knock-in'.")

    if option_type == 'call':
        payoffs = np.maximum(prices[:, -1] - K, 0)
    elif option_type == 'put':
        payoffs = np.maximum(K - prices[:, -1], 0)
    else:
        raise ValueError("Invalid option type. Use 'call' or 'put'.")
    
    if barrier_type == 'knock-out':
        payoffs[barrier_reached] = 0
    elif barrier_type == 'knock-in':
        payoffs[~barrier_reached] = 0
    
    option_price = discount_factor * np.mean(payoffs)
    return option_price


# Example usage:
S0 = 1.25  # Spot price in USD/EUR
K = 1.30  # Strike price
T = 1  # Time to maturity in years
rd = 0.05  # Domestic risk-free interest rate (USD, 5%)
rf = 0.02  # Foreign risk-free interest rate (EUR, 2%)
sigma = 0.20  # Volatility (20%)
Bu = 1.35  # Upper barrier level
Bd = 1.15  # Lower barrier level
n_simulations = 10000  # Number of Monte Carlo simulations
n_steps = 100  # Number of time steps in each simulation

knock_out_call_price_european = simulate_fx_double_barrier_option(S0, K, T, rd, rf, sigma, Bu, Bd, option_type='call', barrier_type='knock-out', style='european', n_simulations=n_simulations, n_steps=n_steps)
knock_out_call_price_american = simulate_fx_double_barrier_option(S0, K, T, rd, rf, sigma, Bu, Bd, option_type='call', barrier_type='knock-out', style='american', n_simulations=n_simulations, n_steps=n_steps)

print(f"Knock-Out Call Option Price (European): {knock_out_call_price_european:.4f}")
print(f"Knock-Out Call Option Price (American): {knock_out_call_price_american:.4f}")


