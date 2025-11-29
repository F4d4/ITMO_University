import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout/Layout';
import VehicleList from './components/Vehicle/VehicleList';
import VehicleCreate from './components/Vehicle/VehicleCreate';
import VehicleEdit from './components/Vehicle/VehicleEdit';
import VehicleDetails from './components/Vehicle/VehicleDetails';
import SpecialOperations from './components/SpecialOperations/SpecialOperations';
import './App.css';

function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Navigate to="/vehicles" replace />} />
          <Route path="/vehicles" element={<VehicleList />} />
          <Route path="/vehicles/create" element={<VehicleCreate />} />
          <Route path="/vehicles/:id" element={<VehicleDetails />} />
          <Route path="/vehicles/:id/edit" element={<VehicleEdit />} />
          <Route path="/special-operations" element={<SpecialOperations />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;






