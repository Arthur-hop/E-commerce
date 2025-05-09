package ourpkg.ecpay;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "CVSStore")
public class CVSStore {

    @XmlElement(name = "CVSStoreID")
    public String CVSStoreID;

    @XmlElement(name = "CVSStoreName")
    public String CVSStoreName;

    @XmlElement(name = "CVSAddress")
    public String CVSAddress;

    @XmlElement(name = "CVSTelephone")
    public String CVSTelephone;

    // 其他欄位你也可以加
}