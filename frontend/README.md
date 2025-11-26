# RCA Insight Engine - Frontend

Modern React + TypeScript frontend for the RCA Insight Engine application.

## Features

- **Search Interface**: Semantic search for RCA documents with AI-powered suggestions
- **Ingestion Management**: Sync and manage Confluence data ingestion
- **Statistics Dashboard**: View system statistics and processing status
- **Real-time Updates**: Auto-refreshing stats and sync status polling

## Tech Stack

- **React 18** with TypeScript
- **Vite** for fast development and building
- **Tailwind CSS** for styling
- **React Router** for navigation
- **Axios** for API communication
- **Lucide React** for icons
- **date-fns** for date formatting

## Getting Started

### Prerequisites

- Node.js 18+ and npm/yarn/pnpm
- Backend API running on `http://localhost:8080`

### Installation

```bash
cd frontend
npm install
```

### Development

```bash
npm run dev
```

The application will be available at `http://localhost:3000`

### Build

```bash
npm run build
```

The production build will be in the `dist` directory.

### Preview Production Build

```bash
npm run preview
```

## Project Structure

```
frontend/
├── src/
│   ├── components/      # Reusable UI components
│   │   └── Layout.tsx   # Main layout with navigation
│   ├── pages/           # Page components
│   │   ├── SearchPage.tsx
│   │   ├── IngestionPage.tsx
│   │   └── StatsPage.tsx
│   ├── services/        # API client and services
│   │   └── api.ts       # API client with TypeScript types
│   ├── App.tsx          # Main app component with routing
│   ├── main.tsx         # Entry point
│   └── index.css        # Global styles
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── tailwind.config.js
```

## API Integration

The frontend communicates with the backend API through the `api.ts` service. All API calls are typed with TypeScript interfaces matching the backend DTOs.

### Environment Variables

Create a `.env` file in the `frontend` directory:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

If not set, the default is `/api` (relative URL, works with Vite proxy).

## Features in Detail

### Search Page

- General semantic search
- Search by symptoms
- Search by root cause
- Configurable top-K results
- AI-generated root cause suggestions with confidence scores
- Similarity scores and detailed RCA information

### Ingestion Page

- Start FULL or INCREMENTAL syncs
- Configure spaces and tags
- Real-time sync status monitoring
- Progress tracking with visual indicators
- Multiple concurrent sync operations

### Statistics Page

- Total pages count
- Success rate calculation
- Status breakdown with progress bars
- Processing pipeline visualization
- Auto-refresh every 30 seconds

## Styling

The application uses Tailwind CSS with a custom color scheme. The primary color is blue (`primary-*` classes).

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Development Notes

- The Vite dev server proxies `/api` requests to `http://localhost:8080`
- Hot Module Replacement (HMR) is enabled for fast development
- TypeScript strict mode is enabled
- ESLint is configured for code quality

## Future Enhancements

- Authentication and authorization
- User preferences and settings
- Advanced filtering and sorting
- Export functionality
- Dark mode
- Responsive mobile design improvements

