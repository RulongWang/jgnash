/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine;

import java.math.BigDecimal;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Investment Performance Summary Class
 * 
 * @author Craig Cavanaugh
 * @version $Id: InvestmentPerformanceSummary.java 3070 2012-01-04 11:00:43Z ccavanaugh $
 */

public class InvestmentPerformanceSummary {

    private Account account;

    private Date startDate;

    private Date endDate;

    private Map<SecurityNode, SecurityPerformanceData> performanceData = new TreeMap<>();

    private List<Transaction> transactions;

    private CurrencyNode baseCurrency;

    public InvestmentPerformanceSummary(final Account account, final boolean recursive) {
        this(account, null, null, recursive);
    }

    private InvestmentPerformanceSummary(final Account account, final Date startDate, final Date endDate, final boolean recursive) {

        if (account == null) {
            throw new IllegalArgumentException("Account may not be null");
        }

        if (!account.memberOf(AccountGroup.INVEST)) {
            throw new IllegalArgumentException("Not a vaild account type");
        }

        this.baseCurrency = account.getCurrencyNode();
        this.account = account;

        if (startDate == null || endDate == null) {
            setStartDate(new Date(1));
            setEndDate(new Date());
        } else {
            setStartDate(startDate);
            setEndDate(endDate);
        }

        transactions = account.getTransactions(getStartDate(), getEndDate());

        if (recursive && account.getChildCount() > 0) {
            collectSubAccountTransactions(account, transactions);
        }

        Collections.sort(transactions);

        runCalculations(recursive);
    }

    private void collectSubAccountTransactions(final Account account, final List<Transaction> transactions) {
        for (Account child : account.getChildren()) {
            transactions.addAll(child.getTransactions(getStartDate(), getEndDate()));

            if (child.getChildCount() > 0) {
                collectSubAccountTransactions(child, transactions);
            }
        }
    }

    private void collectSubAccountSecurities(final Account account, final Set<SecurityNode> securities) {
        for (Account child : account.getChildren()) {
            securities.addAll(child.getSecurities());

            if (child.getChildCount() > 0) {
                collectSubAccountSecurities(child, securities);
            }
        }
    }

    public SecurityPerformanceData getPerformanceData(final SecurityNode node) {
        return performanceData.get(node);
    }

    public List<SecurityNode> getSecurities() {
        return new ArrayList<>(performanceData.keySet());
    }

    /**
     * Calculates the cost basis of a given security which is the average cost including fees
     * 
     * @param data SecurityPerformanceData object to save the result in
     * @param transactions transactions to calculate cost basis against
     */
    private void calculateCostBasis(final SecurityPerformanceData data, final List<Transaction> transactions) {
        SecurityNode node = data.getNode();

        BigDecimal totalShares = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (transaction instanceof InvestmentTransaction) {
                InvestmentTransaction t = (InvestmentTransaction) transaction;

                if (t.getSecurityNode().equals(node)) {

                    BigDecimal rate = baseCurrency.getExchangeRate(t.getInvestmentAccount().getCurrencyNode());

                    BigDecimal fees = t.getFees().multiply(rate);
                    BigDecimal quantity = t.getQuantity();
                    BigDecimal price = t.getPrice().multiply(rate);

                    switch (t.getTransactionType()) {
                        case BUYSHARE:
                        case REINVESTDIV:
                            totalShares = totalShares.add(quantity);
                            totalCost = totalCost.add(price.multiply(quantity).add(fees));
                            break;
                        case SPLITSHARE:
                            totalShares = totalShares.add(quantity);
                            break;
                        case MERGESHARE:
                            totalShares = totalShares.subtract(quantity);
                            break;
                        case DIVIDEND:
                            // do nothing, no fees, no shares added.
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        if (totalShares.compareTo(BigDecimal.ZERO) != 0) {
            data.setCostBasisShares(totalShares);
            data.setCostBasisPerShare(totalCost.divide(totalShares, MathConstants.mathContext));
        }
    }

    /**
     * Calculates the realized gains of a given Security
     * 
     * @param data SecurityPerformanceData object to save the result in
     * @param transactions transactions to calculate the realized gains
     */
    private void calculateRealizedGains(final SecurityPerformanceData data, final List<Transaction> transactions) {
        SecurityNode node = data.getNode();

        BigDecimal totalSharesSold = BigDecimal.ZERO;
        BigDecimal totalSales = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (transaction instanceof InvestmentTransaction) {
                InvestmentTransaction t = (InvestmentTransaction) transaction;

                if (t.getSecurityNode().equals(node)) {

                    BigDecimal rate = baseCurrency.getExchangeRate(t.getInvestmentAccount().getCurrencyNode());

                    BigDecimal fees = t.getFees().multiply(rate);
                    BigDecimal quantity = t.getQuantity();
                    BigDecimal price = t.getPrice().multiply(rate);

                    switch (t.getTransactionType()) {
                        case SELLSHARE:
                            totalSharesSold = totalSharesSold.add(quantity);
                            totalSales = totalSales.add(price.multiply(quantity).subtract(fees));
                            break;
                        case DIVIDEND:
                            totalSales = totalSales.add(t.getTotal(t.getInvestmentAccount()).multiply(rate));
                            break;
                        case REINVESTDIV:
                            totalSales = totalSales.add(t.getTotal(t.getInvestmentAccount()).multiply(rate)).subtract(fees);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        if (totalSharesSold.compareTo(BigDecimal.ZERO) != 0) {
            data.setAvgSalePrice(totalSales.divide(totalSharesSold, MathConstants.mathContext));
            data.setRealizedGains(data.getAvgSalePrice().subtract(data.getCostBasisPerShare()).multiply(totalSharesSold));

        } else if (totalSales.compareTo(BigDecimal.ZERO) != 0) { // pure dividends and no share purchased or sold
            data.setRealizedGains(totalSales);
        }
    }

    private void calculateUnrealizedGains(final SecurityPerformanceData data) {
        if (data.getSharesHeld().compareTo(BigDecimal.ZERO) != 0) {
            data.setUnrealizedGains(data.getPrice().subtract(data.getCostBasisPerShare()).multiply(data.getSharesHeld()));
        }
    }

    private void calculateTotalGains(final SecurityPerformanceData data) {
        data.setTotalGains(data.getUnrealizedGains().add(data.getRealizedGains()));

        if (data.getTotalCostBasis().compareTo(BigDecimal.ZERO) != 0) {
            data.setTotalGainsPercentage(data.getTotalGains().divide(data.getTotalCostBasis(), MathConstants.mathContext));
        }
    }

    /**
     * Sums the number of given security shares
     * 
     * @param data SecurityPerformanceData object to save the result in
     * @param transactions transactions to count shares against
     */
    private void calculateShares(final SecurityPerformanceData data, final List<Transaction> transactions) {
        SecurityNode node = data.getNode();

        BigDecimal shares = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (transaction instanceof InvestmentTransaction) {
                InvestmentTransaction t = (InvestmentTransaction) transaction;

                if (t.getSecurityNode().equals(node)) {
                    switch (t.getTransactionType()) {
                        case ADDSHARE:
                        case BUYSHARE:
                        case REINVESTDIV:
                        case SPLITSHARE:
                            shares = shares.add(t.getQuantity());
                            break;
                        case REMOVESHARE:
                        case SELLSHARE:
                        case MERGESHARE:
                            shares = shares.subtract(t.getQuantity());
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        data.setSharesHeld(data.getSharesHeld().add(shares));
    }

    private void calculatePercentPortfolio() {
        BigDecimal marketValue = BigDecimal.ZERO;

        for (SecurityPerformanceData data : performanceData.values()) {
            marketValue = marketValue.add(data.getMarketValue(account.getCurrencyNode()));
        }

        for (SecurityPerformanceData data : performanceData.values()) {
            if (data.getMarketValue().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal percentage = data.getMarketValue(account.getCurrencyNode()).divide(marketValue, MathConstants.mathContext);

                data.setPercentPortfolio(percentage);
            }
        }
    }

    private void runCalculations(final boolean recursive) {

        Set<SecurityNode> nodes = account.getSecurities();

        if (recursive) {
            collectSubAccountSecurities(account, nodes);
        }

        for (SecurityNode node : nodes) {
            SecurityPerformanceData data = new SecurityPerformanceData(node);

            data.setPrice(getMarketPrice(node, getEndDate()));

            performanceData.put(node, data);

            calculateShares(data, transactions);
            calculateCostBasis(data, transactions);

            calculateRealizedGains(data, transactions);
            calculateUnrealizedGains(data);

            calculateTotalGains(data);
        }

        calculatePercentPortfolio();
    }

    private BigDecimal getMarketPrice(final SecurityNode node, final Date date) {
        return Engine.getMarketPrice(transactions, node, baseCurrency, date);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        Format percentageFormat = NumberFormat.getPercentInstance();
        ((NumberFormat) percentageFormat).setMinimumFractionDigits(2);

        for (SecurityPerformanceData data : performanceData.values()) {
            b.append(data.getNode().getSymbol()).append('\n');
            b.append("sharesHeld: ").append(data.getSharesHeld().toPlainString()).append('\n');
            b.append("price: ").append(data.getPrice().toPlainString()).append('\n');
            b.append("costBasisPerShare: ").append(data.getCostBasisPerShare().toPlainString()).append('\n');
            b.append("costBasisShares: ").append(data.getCostBasisShares().toPlainString()).append('\n');
            b.append("totalCostBasis: ").append(data.getTotalCostBasis().toPlainString()).append('\n');
            b.append("heldCostBasis: ").append(data.getHeldCostBasis().toPlainString()).append('\n');
            b.append("marketValue: ").append(data.getMarketValue().toPlainString()).append('\n');
            b.append("unrealizedGains: ").append(data.getUnrealizedGains().toPlainString()).append('\n');
            b.append("realizedGains: ").append(data.getRealizedGains().toPlainString()).append('\n');
            b.append("totalGains: ").append(data.getTotalGains().toPlainString()).append('\n');
            b.append("totalGainsPercentage: ").append(percentageFormat.format(data.getTotalGainsPercentage())).append('\n');
            b.append("sharesSold: ").append(data.getSharesSold().toPlainString()).append('\n');
            b.append("avgSalePrice: ").append(data.getAvgSalePrice().toPlainString()).append('\n');
            b.append("percentPortfolio: ").append(percentageFormat.format(data.getPercentPortfolio())).append('\n');
            b.append('\n');
        }

        return b.toString();
    }

    private void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    final Date getStartDate() {
        return startDate;
    }

    private void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    final Date getEndDate() {
        return endDate;
    }

    public class SecurityPerformanceData {

        private SecurityNode node;

        private BigDecimal sharesHeld = BigDecimal.ZERO;

        private BigDecimal costBasisPerShare = BigDecimal.ZERO;

        private BigDecimal costBasisShares = BigDecimal.ZERO;

        private BigDecimal avgSalePrice = BigDecimal.ZERO;

        private BigDecimal price = BigDecimal.ZERO;

        private BigDecimal realizedGains = BigDecimal.ZERO;

        private BigDecimal unrealizedGains = BigDecimal.ZERO;

        private BigDecimal totalGains = BigDecimal.ZERO;

        private BigDecimal totalGainsPercentage = BigDecimal.ZERO;

        private BigDecimal percentPortfolio = BigDecimal.ZERO;

        SecurityPerformanceData(final SecurityNode node) {
            setNode(node);
        }

        public BigDecimal getCostBasisPerShare() {
            return costBasisPerShare;
        }

        public BigDecimal getCostBasisShares() {
            return costBasisShares;
        }

        public BigDecimal getMarketValue() {
            return getPrice().multiply(getSharesHeld(), MathConstants.mathContext);
        }

        public BigDecimal getMarketValue(final CurrencyNode currencyNode) {
            return getPrice(currencyNode).multiply(getSharesHeld(), MathConstants.mathContext);
        }

        public SecurityNode getNode() {
            return node;
        }

        public BigDecimal getSharesHeld() {
            return sharesHeld;
        }

        public BigDecimal getPrice(final CurrencyNode currencyNode) {
            return price.multiply(account.getCurrencyNode().getExchangeRate(currencyNode));
        }

        public BigDecimal getPrice() {
            return price;
        }

        public BigDecimal getRealizedGains() {
            return realizedGains;
        }

        public BigDecimal getTotalCostBasis() {
            return getCostBasisPerShare().multiply(getCostBasisShares(), MathConstants.mathContext);
        }

        public BigDecimal getHeldCostBasis() {
            return getCostBasisPerShare().multiply(getSharesHeld(), MathConstants.mathContext);
        }

        public BigDecimal getTotalGains() {
            return totalGains;
        }

        public BigDecimal getTotalGainsPercentage() {
            return totalGainsPercentage;
        }

        public BigDecimal getUnrealizedGains() {
            return unrealizedGains;
        }

        void setCostBasisPerShare(final BigDecimal costBasis) {
            this.costBasisPerShare = costBasis;
        }

        void setCostBasisShares(final BigDecimal costBasisShares) {
            this.costBasisShares = costBasisShares;
        }

        private void setNode(final SecurityNode node) {
            this.node = node;
        }

        void setSharesHeld(final BigDecimal sharesHeld) {
            this.sharesHeld = sharesHeld;
        }

        void setPrice(final BigDecimal price) {
            this.price = price;
        }

        void setAvgSalePrice(final BigDecimal avgSalePrice) {
            this.avgSalePrice = avgSalePrice;
        }

        void setUnrealizedGains(final BigDecimal unrealizedGains) {
            this.unrealizedGains = unrealizedGains;
        }

        void setRealizedGains(final BigDecimal realizedGains) {
            this.realizedGains = realizedGains;
        }

        void setTotalGains(final BigDecimal totalGains) {
            this.totalGains = totalGains;
        }

        void setTotalGainsPercentage(final BigDecimal totalGainsPercentage) {
            this.totalGainsPercentage = totalGainsPercentage;
        }

        public BigDecimal getAvgSalePrice() {
            return avgSalePrice;
        }

        public BigDecimal getSharesSold() {
            return getCostBasisShares().subtract(getSharesHeld());
        }

        public BigDecimal getPercentPortfolio() {
            return percentPortfolio;
        }

        public void setPercentPortfolio(final BigDecimal percentPortfolio) {
            this.percentPortfolio = percentPortfolio;
        }
    }
}