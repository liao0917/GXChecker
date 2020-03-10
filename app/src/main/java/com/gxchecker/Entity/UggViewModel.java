package com.gxchecker.Entity;

import java.util.List;

public class UggViewModel {
    private UggData Ugg ;
    private List<UggQuery> UggQueries ;
    private String Total ;

    public UggData getUgg() {
        return Ugg;
    }

    public void setUgg(UggData ugg) {
        Ugg = ugg;
    }

    public List<UggQuery> getUggQueries() {
        return UggQueries;
    }

    public void setUggQueries(List<UggQuery> uggQueries) {
        UggQueries = uggQueries;
    }

    public String getTotal() {
        return Total;
    }

    public void setTotal(String total) {
        Total = total;
    }
}
