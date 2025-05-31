package server.beans;

public class PointRatioMBean implements PointRatioMXBean {
    
    private final PointStatisticsMBean statisticsMBean;
    
    public PointRatioMBean(PointStatisticsMBean statisticsMBean) {
        this.statisticsMBean = statisticsMBean;
    }
    
    @Override
    public double getMissRatio() {
        int totalPoints = statisticsMBean.getTotalPoints();
        if (totalPoints == 0) {
            return 0.0;
        }
        
        return (double) statisticsMBean.getMissedPoints() / totalPoints * 100.0;
    }
} 