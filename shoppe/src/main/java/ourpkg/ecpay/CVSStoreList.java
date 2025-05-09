package ourpkg.ecpay;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ArrayOfCVSStore")
@XmlAccessorType(XmlAccessType.FIELD)
public class CVSStoreList {

    @XmlElement(name = "CVSStore")
    private List<CVSStore> stores;

    public List<CVSStore> getStores() {
        return stores;
    }

    public void setStores(List<CVSStore> stores) {
        this.stores = stores;
    }
}