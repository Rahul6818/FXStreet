import numpy as np 
from scipy.stats import norm 
def fx_option_price(S, K, T, rd, rf, sigma, option_type='call'): 
    """ 
    Calculate the price of an FX option using the Garman-Kohlhagen model (an extension of Black-Scholes).
    :param S: Current spot price of the underlying asset (e.g., USD/EUR exchange rate) 
    :param K: Strike price of the 
    :option param T: Time to maturity (in years) 
    :param rd: Domestic risk-free interest rate (e.g., USD interest rate) 
    :param rf: Foreign risk-free interest rate (e.g., EUR interest rate) 
    :param sigma: Volatility of the underlying asset (annualized) 
    :param option_type: 'call' for call option, 'put' for put option return: Option price
    """ 
    d1 = (np.log(S / K) + (rd - rf + 0.5 * sigma ** 2) * T) / (sigma * np.sqrt(T)) 
    d2 = d1 - sigma * np.sqrt(T) 
    if option_type == 'call': 
        price = S * np.exp(-rf * T) * norm.cdf(d1) - K * np.exp(-rd * T) * norm.cdf(d2) 
    elif option_type == 'put':
        price = K * np.exp(-rd * T) * norm.cdf(-d2) - S * np.exp(-rf * T) * norm.cdf(-d1) 
    else: raise ValueError("Invalid option type. Use 'call' or 'put'.")
    
    return price
# Example usage:
S = 1.25 # Spot price in USD/EUR 
K = 1.30 # Strike price 
T = 1 # Time to maturity in years 
rd = 0.05 # Domestic risk-free interest rate (USD, 5%) 
rf = 0.02 # Foreign risk-free interest rate (EUR, 2%) 
sigma = 0.20 # Volatility (20%) 
call_price = fx_option_price(S, K, T, rd, rf, sigma, option_type='call') 
put_price = fx_option_price(S, K, T, rd, rf, sigma, option_type='put') 
print(f"Call Option Price: {call_price:.4f}") 
print(f"Put Option Price: {put_price:.4f}")

