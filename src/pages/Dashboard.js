// 📁 src/pages/Dashboard.js
import { useEffect } from "react";
import Chart from "chart.js/auto";

function Dashboard() {
  useEffect(() => {
    const areaCtx = document.getElementById("myAreaChart");
    const barCtx = document.getElementById("myBarChart");

    if (Chart.getChart("myAreaChart")) Chart.getChart("myAreaChart").destroy();
    if (Chart.getChart("myBarChart")) Chart.getChart("myBarChart").destroy();

    if (areaCtx) {
      new Chart(areaCtx, {
        type: "line",
        data: {
          labels: ["Jan", "Feb", "Mar", "Apr", "May", "Jun"],
          datasets: [
            {
              label: "Sales",
              data: [50, 70, 80, 60, 90, 100],
              borderColor: "rgba(75,192,192,1)",
              backgroundColor: "rgba(75,192,192,0.2)",
            },
          ],
        },
      });
    }

    if (barCtx) {
      new Chart(barCtx, {
        type: "bar",
        data: {
          labels: ["A", "B", "C", "D", "E", "F"],
          datasets: [
            {
              label: "Revenue",
              data: [12, 19, 3, 5, 2, 3],
              backgroundColor: "rgba(54,162,235,0.6)",
            },
          ],
        },
      });
    }
  }, []);

  return (
    <div className="container-fluid px-4 pt-3 pb-5 bg-white">
      <h2 className="mb-3 text-center fw-bold">Dashboard</h2>

      {/* 카드 영역 */}
      <div className="row gx-2 gy-2">
        {[
          { color: "primary", text: "Primary" },
          { color: "warning", text: "Warning" },
          { color: "success", text: "Success" },
          { color: "danger", text: "Danger" },
        ].map((c) => (
          <div className="col-6" key={c.text}>
            <div className={`card bg-${c.color} text-white`}>
              <div className="card-body text-center">{c.text}</div>
            </div>
          </div>
        ))}
      </div>

      {/* 그래프 영역 */}
      <div className="mt-4">
        <div className="card mb-3">
          <div className="card-header fw-semibold">
            <i className="fas fa-chart-area me-1"></i> Area Chart
          </div>
          <div className="card-body">
            <canvas id="myAreaChart" width="100%" height="40"></canvas>
          </div>
        </div>
        <div className="card">
          <div className="card-header fw-semibold">
            <i className="fas fa-chart-bar me-1"></i> Bar Chart
          </div>
          <div className="card-body">
            <canvas id="myBarChart" width="100%" height="40"></canvas>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;