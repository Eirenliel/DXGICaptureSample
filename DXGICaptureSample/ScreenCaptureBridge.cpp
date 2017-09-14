// ScreenCaptureBridge.cpp : Defines the entry point for the console application
// Creates a shared memory file with screen buffer

#include "stdafx.h"
#include "DXGIManager.h"

DXGIManager g_DXGIManager;

int _tmain(int argc, _TCHAR* argv[])
{
	printf("Screen capture bridge\n");
	if (argc < 3) {
		printf("Program arguments: <file name> <monitor 1|2|0>\n");
		return -1;
	}

	TCHAR* fileName = argv[1];

	CaptureSource cs = CSDesktop;
	std::wstring captureId = argv[2];
	if (captureId.compare(_T("1")) == 0) {
		cs = CSMonitor1;
	}
	else if (captureId.compare(_T("2")) == 0) {
		cs = CSMonitor2;
	}

	HANDLE hDestFile = CreateFile(
		fileName,
		GENERIC_WRITE | GENERIC_READ,
		FILE_SHARE_READ | FILE_SHARE_WRITE,
		NULL,
		CREATE_ALWAYS,
		FILE_ATTRIBUTE_NORMAL,
		NULL);
	if (hDestFile == NULL) {
		printf("%d\n", GetLastError());
		return 1;
	}

	CoInitialize(NULL);

	g_DXGIManager.SetCaptureSource(cs);

	RECT rcDim;
	g_DXGIManager.GetOutputRect(rcDim);

	DWORD dwWidth = rcDim.right - rcDim.left;
	DWORD dwHeight = rcDim.bottom - rcDim.top;

	DWORD dwBufSize = dwWidth*dwHeight * 4;

	CComPtr<IWICImagingFactory> spWICFactory = NULL;
	HRESULT hr = spWICFactory.CoCreateInstance(CLSID_WICImagingFactory);
	if (FAILED(hr))
		return hr;

	HANDLE hMapFile;
	LPVOID mapObject;

	hMapFile = CreateFileMapping(
		hDestFile,    // use paging file
		NULL,                    // default security
		PAGE_READWRITE,          // read/write access
		0,                       // maximum object size (high-order DWORD)
		dwBufSize,                // maximum object size (low-order DWORD)
		NULL);                 // name of mapping object

	if (hMapFile == NULL)
	{
		_tprintf(_T("Could not create file mapping object (%d).\n"),
			GetLastError());
		return 1;
	}
	mapObject = MapViewOfFile(hMapFile,   // handle to map object
		FILE_MAP_ALL_ACCESS, // read/write permission
		0,
		0,
		dwBufSize);

	if (mapObject == NULL)
	{
		_tprintf(_T("Could not map view of file (%d).\n"),
			GetLastError());

		CloseHandle(hMapFile);

		return 1;
	}
	_tprintf(_T("Capturing screen %d of size %dx%d to file %s\n"), cs, dwWidth, dwHeight, fileName);
	while (true) {
		//long int start = GetTickCount();
		int i = 0;
		do
		{
			hr = g_DXGIManager.GetOutputBits((BYTE *) mapObject, rcDim);
			i++;
		} while (hr == DXGI_ERROR_WAIT_TIMEOUT);

		if (FAILED(hr))
		{
			printf("GetOutputBits failed with hr=0x%08x\n", hr);
			return hr;
		}
		//long int end = GetTickCount();
		//printf("Screen captured in %d ms\n", end - start);
	}
	
	UnmapViewOfFile(mapObject);
	CloseHandle(hMapFile);

	return 0;
}

